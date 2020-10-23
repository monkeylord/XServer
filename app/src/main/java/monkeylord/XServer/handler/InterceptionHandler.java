package monkeylord.XServer.handler;

import java.io.File;
import java.util.HashMap;

import monkeylord.XServer.handler.Hook.XServer_MethodHook;

public class TraceHandler {
    static TraceHandler instance = null;
    String[] shouldTrace = new String[]{};
    File traceFile;
    HashMap<String, XServer_MethodHook> hooks = new HashMap<>();

    TraceHandler(){

    }
    static TraceHandler getInstance(){
        if(instance==null)instance = new TraceHandler();
        return instance;
    }

    static 



}
