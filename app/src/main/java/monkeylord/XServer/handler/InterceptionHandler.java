package monkeylord.XServer.handler;

import java.io.File;
import java.util.HashMap;

import monkeylord.XServer.handler.Hook.XServer_MethodHook;

public class InterceptionHandler {
    static InterceptionHandler instance = null;
    String[] shouldTrace = new String[]{};
    File traceFile;
    HashMap<String, XServer_MethodHook> hooks = new HashMap<>();

    InterceptionHandler(){

    }
    static InterceptionHandler getInstance(){
        if(instance==null)instance = new InterceptionHandler();
        return instance;
    }




}
