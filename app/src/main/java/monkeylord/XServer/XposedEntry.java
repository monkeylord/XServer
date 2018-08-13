package monkeylord.XServer;

import android.content.pm.ApplicationInfo;
import android.content.res.XModuleResources;
import android.os.Process;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


/*
    某些Android 4版本，需要修改依赖库的配置才能兼容，否则会报pre-verifed错误。
	原因：Framework也提供了XposedBridgeApi，和编译进插件的内容重复。所以要把XposedBridgeApi从编译改为引用。
	修改：Build->Edit Libraries and Dependencies  将XposedBridgeApi的scope从compile改为provided

*/

public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static ClassLoader classLoader;
    public static XModuleResources res;
    String targetApp = new XSharedPreferences(this.getClass().getPackage().getName().toLowerCase(), "XServer").getString("targetApp", "monkeylord.demoapp");
    String packageName;
    Boolean isFirstApplication;
    String processName;
    ApplicationInfo appInfo;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        //告知界面模块已启动
        if (loadPackageParam.packageName.equals("monkeylord.xserver"))
            XposedHelpers.findAndHookMethod("monkeylord.XServer.MainActivity", loadPackageParam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        //从SharedPreferences中刷新目标APP名称
        targetApp = new XSharedPreferences(this.getClass().getPackage().getName().toLowerCase(), "XServer").getString("targetApp", "monkeylord.demoapp");
        if (!loadPackageParam.packageName.equals(targetApp)) return;
        gatherInfo(loadPackageParam);
        //Write your code here.
        new XServer(8000);
        new XServer(Process.myPid());
        XposedBridge.log("XServer Listening...");
    }


    private void gatherInfo(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        packageName = loadPackageParam.packageName;
        isFirstApplication = loadPackageParam.isFirstApplication;
        classLoader = loadPackageParam.classLoader;
        processName = loadPackageParam.processName;
        appInfo = loadPackageParam.appInfo;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        res = XModuleResources.createInstance(startupParam.modulePath, null);
    }
}
