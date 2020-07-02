package monkeylord.XServer;

import android.content.pm.ApplicationInfo;
import android.content.res.XModuleResources;
import android.os.Build;
import android.os.Process;

import java.lang.reflect.Member;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import monkeylord.XServer.handler.Hook.Unhook;
import monkeylord.XServer.handler.Hook.XServer_MethodHook;
import monkeylord.XServer.handler.Hook.XServer_Param;
import monkeylord.XServer.handler.HookHandler;

/*
    某些Android 4版本，需要修改依赖库的配置才能兼容，否则会报pre-verifed错误。
	原因：Framework也提供了XposedBridgeApi，和编译进插件的内容重复。所以要把XposedBridgeApi从编译改为引用。
	修改：Build->Edit Libraries and Dependencies  将XposedBridgeApi的scope从compile改为provided

*/

public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static boolean debug=true;
    public static ClassLoader classLoader;
    public static XModuleResources res;
    public static XSharedPreferences sPrefs;
    String packageName;
    Boolean isFirstApplication;
    String processName;
    ApplicationInfo appInfo;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        res = XModuleResources.createInstance(startupParam.modulePath, null);
        sPrefs = new XSharedPreferences(this.getClass().getPackage().getName().toLowerCase(), "XServer");
        sPrefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        //告知界面模块已启动，同时解除Android N以上对MODE_WORLD_READABLE的限制
        if (loadPackageParam.packageName.equals("monkeylord.xserver")) {
            XposedHelpers.findAndHookMethod("monkeylord.XServer.MainActivity", loadPackageParam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
            if (Build.VERSION.SDK_INT >= 24)XposedHelpers.findAndHookMethod("android.app.ContextImpl", loadPackageParam.classLoader, "checkMode",int.class, XC_MethodReplacement.returnConstant(null));
            XposedBridge.log("XServer handleLoadPackage: "+ Build.VERSION.SDK_INT);
        }
        //获取目标包名
        sPrefs.reload();
        String targetApp = sPrefs.getString("targetApp", "MadMode");
        //if(targetApp.equals("MadMode"))XposedBridge.log("XServer Cannot Figure Out TargetApp...Hooking Everyone Now!!");
        if (!targetApp.equals("MadMode")&&!loadPackageParam.packageName.equals(targetApp)) return;
        gatherInfo(loadPackageParam);
        //启动XServer
        if(!targetApp.equals("MadMode"))new XServer(8000);
        new XServer(Process.myPid());
        XposedBridge.log("XServer Listening... on"+loadPackageParam.packageName + "@" + Process.myPid());
        setXposedHookProvider();
        XposedBridge.log("Using XposedHook...@" + Process.myPid());
    }

    void setXposedHookProvider(){
        XServer.classLoader = classLoader;
        XServer.assetManager = res.getAssets();
        HookHandler.setProvider(new HookHandler.HookProvider() {
            // 复用Hook，否则在大量Hook时会OOM
            HashMap<XServer_MethodHook, XC_MethodHook> pairs = new HashMap<>();
            @Override
            public Unhook hookMethod(Member hookMethod, final XServer_MethodHook mycallback) {
                XC_MethodHook myCallback = pairs.get(mycallback);
                if(myCallback==null)myCallback = new XC_MethodHook() {
                    XServer_MethodHook callback = mycallback;
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        XServer_Param xsParam = new XServer_Param();
                        xsParam.args = param.args==null ? new Object[0] : param.args;
                        xsParam.method = param.method;
                        xsParam.thisObject = param.thisObject;
                        xsParam.throwable = param.getThrowable();
                        xsParam.result = param.getResult();
                        callback.beforeHookedMethod(xsParam);
                        if (xsParam.returnEarly) {
                            if (xsParam.hasThrowable()) param.setThrowable(xsParam.getThrowable());
                            else param.setResult(xsParam.result);
                        }
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        XServer_Param xsParam = new XServer_Param();
                        xsParam.args = param.args;
                        xsParam.method = param.method;
                        xsParam.thisObject = param.thisObject;
                        xsParam.throwable = param.getThrowable();
                        xsParam.result = param.getResult();
                        callback.afterHookedMethod(xsParam);
                        if (xsParam.hasThrowable()) param.setThrowable(xsParam.getThrowable());
                        else param.setResult(xsParam.result);
                    }
                };
                pairs.put(mycallback,myCallback);
                Object unhook = XposedBridge.hookMethod(hookMethod, myCallback);
                return new Unhook(hookMethod, unhook);
            }

            @Override
            public void unhookMethod(Member hookMethod, Object additionalObj) {
                if(additionalObj!=null)((XC_MethodHook.Unhook)additionalObj).unhook();
            }
        });
    }

    private void gatherInfo(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        packageName = loadPackageParam.packageName;
        isFirstApplication = loadPackageParam.isFirstApplication;
        classLoader = loadPackageParam.classLoader;
        processName = loadPackageParam.processName;
        appInfo = loadPackageParam.appInfo;
    }
}
