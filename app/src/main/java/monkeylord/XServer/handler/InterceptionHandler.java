package monkeylord.XServer.handler;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import monkeylord.XServer.api.Invoke_New;
import monkeylord.XServer.handler.Hook.Unhook;
import monkeylord.XServer.handler.Hook.XServer_MethodHook;
import monkeylord.XServer.handler.Hook.XServer_Param;
import monkeylord.XServer.utils.Utils;
import monkeylord.XServer.utils.netUtil;

public class InterceptionHandler {
    static InterceptionHandler instance = null;
    ArrayList<String> shouldIntercept = new ArrayList();
    ArrayList<String> methods = new ArrayList<>();
    File logFile;
    HashMap<String, Unhook> hooks = new HashMap<String, monkeylord.XServer.handler.Hook.Unhook>();
    Interceptor interceptor;

    final String server = "http://127.0.0.1:8000";

    InterceptionHandler(){
        interceptor = new Interceptor();
        try {
            Class DexFile = Class.forName("dalvik.system.DexFile");
            Method defineClass = DexFile.getDeclaredMethod("defineClass",String.class, ClassLoader.class, long.class, List.class);
            HookHandler.getProvider().hookMethod(defineClass, new XServer_MethodHook() {
                @Override
                public void afterHookedMethod(XServer_Param param) throws Throwable {
                    super.afterHookedMethod(param);
                    if(param.getResult()==null)return;
                    Log.e("TEST", param.args[0].toString());
                    if(shouldIntercept.contains(param.args[0].toString())){
                        Class loadedClass = (Class) param.getResult();
                        for (Method method : loadedClass.getDeclaredMethods()) {
                            if(methods.contains(Utils.getJavaName(method)))intercept(method);
                        }
                    }
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    public void setInterceptMethods(String[] itcpMethods){
        for (String itcpMethod : itcpMethods) {
            shouldIntercept.add(itcpMethod.substring(itcpMethod.indexOf("L")+1,itcpMethod.indexOf(";")));
            methods.add(itcpMethod);
        }
    }

    public static InterceptionHandler getInstance(){
        if(instance==null)instance = new InterceptionHandler();
        return instance;
    }

    public void intercept(Method method){
        String javaname = Utils.getJavaName(method);
        Log.e("TEST", "intercept: "+javaname);
        if(hooks.get(javaname)!=null)return;
        else hooks.put(javaname,HookHandler.getProvider().hookMethod(method, interceptor));
    }

    public void stopIntercept(Method method){
        String javaname = Utils.getJavaName(method);
        if(hooks.get(javaname)==null)return;
        else hooks.remove(javaname).unhook();
    }

    class Interceptor extends XServer_MethodHook{
        @Override
        public void beforeHookedMethod(XServer_Param param) throws Throwable {
            super.beforeHookedMethod(param);
            if(Invoke_New.isMe())return;

            JSONObject call = new JSONObject();
            JSONArray params = new JSONArray();
            for (Object arg:param.args) { params.add(ObjectHandler.saveObject(arg)); }
            call.put("method", Utils.getJavaName((Method) param.method));
            if(param.thisObject!=null)call.put("this",ObjectHandler.saveObject(param.thisObject));
            call.put("params",params);
            ArrayList stacks=new ArrayList();
            for (StackTraceElement element:Thread.currentThread().getStackTrace()) {
                stacks.add(element.getClassName() + "." + element.getMethodName() + " : " + element.getLineNumber());
            }
            call.put("stackTrace",stacks);
            Object result = ObjectHandler.parseObject(new netUtil(server + "/invoke2", new org.json.JSONObject(call.toJSONString()).toString(2)).getRet());
            Log.e("[XServer Debug]", "result: " + result);
            if(result instanceof Invoke_New.XServerWrappedThrowable){
                if(((Invoke_New.XServerWrappedThrowable)result).shouldPassthough)
                    param.setThrowable(((Invoke_New.XServerWrappedThrowable)result).getThrowable());
            }
            else param.setResult(result);
        }
    }



}
