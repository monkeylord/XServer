/*
XServer Frida 加载脚本

功能：使用Frida在目标应用中加载并实现XServer
特点：无需Xposed框架
原理：使用Frida创建并注册XServer的Hook Provider
用法：frida -U [app] -l XServer.js
*/

/*
Hook Manager
{
    method:[hook]
}
*/
var Hooks = {}
var typeTranslation = {
    "Z":"java.lang.Boolean",
    "B":"java.lang.Byte",
    "S":"java.lang.Short",
    "I":"java.lang.Integer",
    "J":"java.lang.Long",
    "F":"java.lang.Float",
    "D":"java.lang.Double"
}
var XServerCL = null
var XServerFactory = null

const DEBUG = false

/*
核心方法
技术原理：使用Frida注册符合该接口的Provider类
*/
function registerHookProvider(){
    var provideInterface = XServerFactory.use("monkeylord.XServer.handler.HookHandler$HookProvider")
    var provider = XServerFactory.registerClass({
        name: 'FridaHookProvider',
        implements: [provideInterface],
        methods: {
            hookMethod: [{
                returnType: 'monkeylord.XServer.handler.Hook.Unhook',
                argumentTypes: ['java.lang.reflect.Member', 'monkeylord.XServer.handler.Hook.XServer_MethodHook'],
                implementation: function (hookMethod, mycallback) {
                    
                    // 反射方法在Frida中无法使用，需要通过Frida得到这个方法
                    console.log("[XServer] Hook ", hookMethod.getName(), "with", mycallback)

                    var refMethod = Java.use(hookMethod.$className)
                    var method = Java.cast(hookMethod, refMethod)
                    var clazz = method.getDeclaringClass().getName()
                    var mtdname = (hookMethod.$className=="java.lang.reflect.Constructor")? "$init": method.getName()
                    var overload = method.getParameterTypes().map(function(clz){return clz.getName()})
                    var fridaMethod = Java.use(clazz)[mtdname].overload.apply(Java.use(clazz)[mtdname], overload)
                    
                    // 现在我们拿到了Frida方法，以及需要的信息
                    
                    // 开始注册Hook
                    // XServer可能会大量Hook，所以这里必须复用Hook以节省内存开销
                    
                    // fullname是方法的唯一标识符
                    var fullname = clazz+"->"+mtdname+"("+overload.join(",")+")"
                    // 保存这个反射方法的全局引用，之后有用
                    var paramMethod = Java.retain(hookMethod)
                    
                    // 注册Callbacks
                    if(!Hooks[fullname]){
                        // New Hook
                        Hooks[fullname] = [Java.retain(mycallback)]
                    }else{
                        // 筛选过滤，如果已经有了就不要重复Hook
                        var isNewCallback = Hooks[fullname].every(function(callback){
                            if(DEBUG)console.log("isEqual: " + callback.equals(mycallback))
                            return !callback.equals(mycallback)
                        })
                        
                        if(isNewCallback)Hooks[fullname].push(Java.retain(mycallback))
                    }
                    
                    // 实施Hook
                    fridaMethod.implementation = function(){
                        console.log("[XServer]", "Hooked method called:", paramMethod.getName())
                        console.log("[XServer]", "Receiver:", this)
                        console.log("[XServer]", "Hooks", Hooks[fullname])
                        
                        var param = XServerFactory.use("monkeylord.XServer.handler.Hook.XServer_Param").$new()
                        param.method.value = paramMethod
                        param.thisObject.value = (fridaMethod.type == 3)? this : null
                        // 准备参数
                        var args = arguments
                        var jarr = Object.keys(arguments).map(function(key){return args[key]})
                        
                        fridaMethod.argumentTypes.forEach(function(type,index){
                            var env = Java.vm.getEnv()
                            if(DEBUG){
                                console.log("[DECBUG]ArgInfo "+index)
                                console.log(JSON.stringify(type))
                                console.log(jarr[index])
                                console.log(JSON.stringify(type.toJni(jarr[index], env)))
                                console.log(type.toJni(jarr[index], env).isNull)
                            }
                            if(jarr[index]==null)jarr[index] = null
                            else if(type.type != "pointer")jarr[index] = Java.use(typeTranslation[type.name]).valueOf(jarr[index])
                            else if(type.className == "java.lang.String")jarr[index]=Java.use("java.lang.String").$new(jarr[index])
                            else jarr[index] = Java.classFactory._getType("java.lang.Object").fromJni(type.toJni(jarr[index], env), env, false)
                        })
                        
                        param.args.value = Java.array("java.lang.Object", jarr)
                        // 开始调用Callbacks
                        
                        for(var i = 0; i<Hooks[fullname].length; i++){
                            try{
                                Hooks[fullname][i].beforeHookedMethod(param)
                            }catch(e){
                                console.log(e)
                            }
                            if(param.returnEarly.value)break;
                        }
                        
                        
                        // 处理原方法调用
                        if(DEBUG){
                            console.log("[DEBUG]", "Unwrap arguments")
                            console.log("[DEBUG]", "arguments length", param.args.value.length)
                            console.log("[DEBUG]", "arguments array", JSON.stringify(param.args.value))
                            param.args.value.forEach(function(arg, index){
                                console.log("[DEBUG]", "argument"+index, JSON.stringify(arg))
                            })
                        }

                        var newargs = []
                        for(var i = 0; i<param.args.value.length; i++)newargs[i] = param.args.value[i]
                        
                        fridaMethod.argumentTypes.forEach(function(type,index){
                            if(newargs[index]==null)newargs[index] = null
                            else if(type.type!="pointer"){
                                //console.log("CAST: ",JSON.stringify(Object.keys(jarr[index])))
                                var value
                                var basicObj = Java.cast(jarr[index],Java.use(typeTranslation[type.name]))
                                console.log(JSON.stringify(type))
                                console.log(JSON.stringify(basicObj))
                                switch(type.name){
                                    case "Z":
                                        value = basicObj.booleanValue();break;
                                    case "B":
                                        value = basicObj.byteValue();break;
                                    case "S":
                                        value = basicObj.shortValue();break;
                                    case "I":
                                        value = basicObj.intValue();break;
                                    case "J":
                                        value = basicObj.longValue();break;
                                    case "F":
                                        value = basicObj.floatValue();break;
                                    case "D":
                                        value = basicObj.doubleValue();break;
                                }
                                newargs[index]=value
                            }else if(type.name.startsWith("[")){
                                var env = Java.vm.getEnv()
                                newargs[index] = type.fromJni(Java.classFactory._getType("java.lang.Object").toJni(newargs[index], env), env, true)
                            }else{
                                newargs[index] = Java.cast(newargs[index], Java.use(newargs[index].$className))
                            }
                        })
                        
                        try{
                            if(!param.returnEarly.value){
                                var env = Java.vm.getEnv()
                                if(DEBUG){
                                    console.log("[DEBUG]", "Invoking original method", fridaMethod.methodName)
                                    console.log("[DEBUG]", "Method type:", fridaMethod.type)
                                    newargs.forEach(function(arg, index){
                                        console.log("[DEBUG]","argument"+index, JSON.stringify(arg))
                                    })
                                }
                                var result = fridaMethod.apply((fridaMethod.type == 3)? Java.cast(param.thisObject.value, Java.use(this.$className)): Java.use(clazz), newargs)
                                if(DEBUG){
                                    console.log("[DEBUG] Original Returned Object:"+ JSON.stringify(result))
                                }
                                var resultObject
                                if(result == undefined)resultObject = null
                                else if(fridaMethod._p[4].type!="pointer")resultObject = Java.use(typeTranslation[fridaMethod._p[4].name]).valueOf(result)
                                else resultObject = Java.classFactory._getType("java.lang.Object").fromJni(fridaMethod._p[4].toJni(result, env), env, false)
                                param.setResult(resultObject)  // Call original
                            }
                        }catch(e){
                            console.log(e)
                            if(e.getCause){
                                param.setThrowable(e.getCause())
                            }
                        }
                        //param.result.value = "123"
                        
                        
                        for(var i = Hooks[fullname].length - 1; i >= 0; i--){
                            var lastResult = param.getResult()
                            var lastThrowable = param.getThrowable()
                            try{
                                Hooks[fullname][i].afterHookedMethod(param)
                            }catch(e){
                                console.log(e)
                                if (lastThrowable == null) {
                                    param.setResult(lastResult);
                                } else {
                                    param.setThrowable(lastThrowable);
                                }
                            }
                            //if(param.returnEarly.value)break;
                        }
                        
                        //var r = Hooks[fullname][0].afterHookedMethod(param)
                        if(DEBUG){
                            var env = Java.vm.getEnv()
                            console.log("[DEBUG]","Return early:",param.returnEarly.value)
                            //console.log("[DEBUG]","Return Object Type:",JSON.stringify(param.result.value.getClass()))
                            console.log("[DEBUG]","Return Object:", JSON.stringify(param.result.value))
                            console.log("[DEBUG]","Return Object Original Type:",JSON.stringify(fridaMethod._p[4]))
                            if(fridaMethod._p[4].name.startsWith("["))console.log("[DEBUG]Return Object Casted:"+ JSON.stringify(fridaMethod._p[4].fromJni(Java.classFactory._getType("java.lang.Object").toJni(param.result.value, env), env, false)))
                        }
                        var returnObject
                        if(fridaMethod._p[4].type=="void"){
                            returnObject = undefined
                        }else if(fridaMethod._p[4].name.startsWith("[")&&!fridaMethod._p[4].name.startsWith("[L")){
                            returnObject = fridaMethod._p[4].fromJni(Java.classFactory._getType("java.lang.Object").toJni(param.result.value, env), env, false)
                        }else{
                            returnObject = param.result.value
                        }
                        return returnObject
                        //return (fridaMethod._p[4].type=="void") ? undefined : param.result.value
                    }
                    
                    var unhook = XServerFactory.use("monkeylord.XServer.handler.Hook.Unhook").$new(hookMethod, mycallback)
                    
                    return unhook
                }
            }],
            unhookMethod: [{
                returnType: 'void',
                argumentTypes: ['java.lang.reflect.Member', 'java.lang.Object'],
                implementation: function (hookMethod, additionalObj) {
                    // 反射方法在Frida中无法使用，从Frida得到这个方法
                    // Frida Hook里additionalObj是对应的callback
                    console.log("Unhook")
                    var refMethod = Java.use(hookMethod.$className)
                    var method = Java.cast(hookMethod, refMethod)
                    var clazz = method.getDeclaringClass().getName()
                    var mtdname = (hookMethod.$className=="java.lang.reflect.Constructor")? "$init": method.getName()
                    var overload = method.getParameterTypes().map(function(clz){return clz.getName()})
                    
                    // fullname是方法的唯一标识符
                    var fullname = clazz+"->"+mtdname+"("+overload.join(",")+")"
                    if(Hooks[fullname]){
                        Hooks[fullname] = Hooks[fullname].filter(function(callback){
                            console.log("Comparing...")
                            callback.equals(additionalObj)
                        })
                    }
                }
            }]
        }
    });
    console.log("[XServer]HookProvider created:" + provider)
    //var myprovider = Java.ClassFactory.cast(provider, provideInterface)
    //console.log("casted")
    var HookHandler = XServerFactory.use("monkeylord.XServer.handler.HookHandler");
    //console.log(myprovider)
    HookHandler.setProvider(provider.$new())
    console.log("[XServer]HookProvider registered")
}

/*
功能性方法：完成并调用XServer的加载和启动流程
*/

function loadXServer(path){
    // Fix CodeCache, in case App does not have file permission
    var ActivityThread = Java.use("android.app.ActivityThread")
    var app = ActivityThread.currentApplication()
    Java.classFactory.cacheDir = "/data/data/" + app.getPackageName() + "/cache"
    Java.classFactory.codeCacheDir = "/data/data/" + app.getPackageName() + "/code_cache"
    
    // Load
    //Java.openClassFile(path).load()
    var DexClassLoader = Java.use("dalvik.system.DexClassLoader");
    XServerCL = DexClassLoader.$new(path, Java.classFactory.codeCacheDir, null, DexClassLoader.getSystemClassLoader());
    XServerFactory = Java.ClassFactory.get(XServerCL)
    XServerFactory.cacheDir = "/data/data/" + app.getPackageName() + "/cache"
    XServerFactory.codeCacheDir = "/data/data/" + app.getPackageName() + "/code_cache"

    var XServer = XServerFactory.use("monkeylord.XServer.XServer");
    console.log("[XServer]XServer loaded in APP: " + XServer)
}

function setAsset(path){
    var AssetManager = Java.use("android.content.res.AssetManager")
    var assets = AssetManager.$new()
    assets.addAssetPath(path)
    var XServer =  XServerFactory.use("monkeylord.XServer.XServer");
    XServer.assetManager.value = assets
    console.log("[XServer]XServer Assets loaded from " + path)
}

function setClassLoader(){
    var XServer = XServerFactory.use("monkeylord.XServer.XServer");
    var ActivityThread = Java.use("android.app.ActivityThread")
    console.log("[XServer]ClassLoader used by currentApplication:" + ActivityThread.currentApplication().getClassLoader())
    XServer.classLoader.value = ActivityThread.currentApplication().getClassLoader()
}

function startXServer(port){
    var XServer = XServerFactory.use("monkeylord.XServer.XServer");
    XServer.$new(port)
    console.log("[XServer]HttpService started at " + port)
    console.log("[XServer]Tips: adb forward tcp:8000 tcp:"+port)
    console.log("[XServer]Access XServer at http://127.0.0.1:8000/")
}

Java.performNow(function(){
    console.log("XServer Frida Tool")
    console.log("By Monkeylord")
    var javaFile = Java.use("java.io.File")
    var possiblePaths = [
        "/data/app/monkeylord.xserver-1/base.apk",
        "/data/app/monkeylord.xserver-2/base.apk",
        "/data/local/tmp/xserver.apk"
    ]
    var paths = possiblePaths.filter(function(path){
        return javaFile.$new(path).exists()
    })
    
    if(paths.length == 0){
        console.log("XServer APK not found")
        console.log("Please make sure XServer is installed, or at '/data/local/tmp/xserver.apk'")
        console.log("You can 'adb push [XServer APK Path] /data/local/tmp/xserver.apk' if you don't want to install XServer")
        return
    }
    //var path = "/data/local/tmp/xserver.apk"
    //var path = "/data/app/monkeylord.xserver-1/base.apk"
    var path = paths[0]
    console.log("[XServer]Loading XServer from "+ path)
    loadXServer(path)
    registerHookProvider()
    setAsset(path)
    setClassLoader()
    startXServer(8000)
})