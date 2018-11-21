package monkeylord.XServer;

import android.content.pm.ApplicationInfo;
import android.content.res.XModuleResources;
import android.os.MemoryFile;
import android.os.Process;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.netUtil;


/*
    某些Android 4版本，需要修改依赖库的配置才能兼容，否则会报pre-verifed错误。
	原因：Framework也提供了XposedBridgeApi，和编译进插件的内容重复。所以要把XposedBridgeApi从编译改为引用。
	修改：Build->Edit Libraries and Dependencies  将XposedBridgeApi的scope从compile改为provided

*/

public class XposedEntry implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    public static boolean debug=true;
    public static ClassLoader classLoader;
    public static XModuleResources res;
    static File sharedFile;
    static MemoryFile memFile;
    String targetApp = new XSharedPreferences(this.getClass().getPackage().getName().toLowerCase(), "XServer").getString("targetApp", "monkeylord.demoapp");
    String packageName;
    Boolean isFirstApplication;
    String processName;
    ApplicationInfo appInfo;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        //告知界面模块已启动
        if (loadPackageParam.packageName.equals("monkeylord.xserver")) {
            XposedHelpers.findAndHookMethod("monkeylord.XServer.MainActivity", loadPackageParam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
            XposedHelpers.findAndHookMethod("monkeylord.XServer.MainActivity", loadPackageParam.classLoader, "getFile", XC_MethodReplacement.returnConstant(sharedFile));
        }
        //System Server方法（这次应该兼容MIUI了）
        if(loadPackageParam.packageName.equals("android")){
            //systemServer();
            return;
        }else targetApp=new netUtil("http://127.0.0.1:7999/","",null).getRet();
        //XposedBridge.log(targetApp);
        //刷新目标APP名称
        //从共享文件中刷新目标APP名称（For Android 7.0, MIUI not compatible, sad...）
        /*
        String memApp=new BufferedReader(new InputStreamReader(memFile.getInputStream())).readLine().trim();
        if(memApp.length()!=0)targetApp=memApp;
        else memFile.writeBytes(targetApp.getBytes(),0,0,targetApp.length());
        */
        //从共享文件中刷新目标APP名称（For Android 7.0）
        //targetApp=new BufferedReader(new FileReader(sharedFile)).readLine();
        //从XPreferences中刷新目标APP名称（Unavailable in Android 7.0）
        //targetApp = new XSharedPreferences(this.getClass().getPackage().getName().toLowerCase(), "XServer").getString("targetApp", "monkeylord.demoapp");
        if (!loadPackageParam.packageName.equals(targetApp)) return;
        gatherInfo(loadPackageParam);
        //启动XServer
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
        //内存文件
        /*
        try {
            memFile=new MemoryFile("xserver",512);
            memFile.allowPurging(false);
            targetApp=targetApp+"\r\n";
            memFile.writeBytes(targetApp.getBytes(), 0, 0, targetApp.length());
        }catch (IOException e){
            XposedBridge.log("MemoryFile 写入异常");
        }
        */
        //临时文件
        /*
        BufferedWriter writer;
        String filePath="/data/data/"+this.getClass().getPackage().getName().toLowerCase()+"/XServer.conf";
        XposedBridge.log(filePath);

        sharedFile=new File(filePath);
        if (!sharedFile.exists()){
            sharedFile.createNewFile();
            Runtime.getRuntime().exec("chmod 777 "+filePath);
        }
        //targetApp=new BufferedReader(new FileReader(sharedFile)).readLine();
        //if(targetApp==null)targetApp="com.example";
        //sharedFile.setReadable(true);
        //sharedFile.setWritable(true);
        //sharedfd=new FileInputStream(sharedFile).getFD();
        writer=new BufferedWriter(new FileWriter(sharedFile));
        writer.write(targetApp);
        writer.flush();
        */
    }
}
