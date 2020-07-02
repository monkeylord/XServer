package monkeylord.XServer.api;

import android.os.Process;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.Hook.Unhook;
import monkeylord.XServer.handler.Hook.XServer_MethodHook;
import monkeylord.XServer.handler.Hook.XServer_Param;
import monkeylord.XServer.handler.HookHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.DexHelper;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.NanoWSD;
import monkeylord.XServer.utils.Utils;

//MassTracer的WebSocket处理重构，交互式。
//TODO 完成增删改查，兼容之前接口，然后替换。
public class wsTracerNew implements XServer.wsOperation {
    final boolean unhookOnClose = true;
    HashMap<String, Unhook> unhooks = new HashMap<>();
    ws websocket;
    hook hook;

    @Override
    public NanoWSD.WebSocket handle(NanoHTTPD.IHTTPSession handshake) { websocket = new ws(handshake);hook = new hook();return websocket; }

    public void handleMessage(String msg){
        JSONObject req = JSON.parseObject(msg);
        try {
            JSONObject call = new JSONObject();
            String[] methods ={};
            String[] classes = {};
            ArrayList<String> clzArr = new ArrayList<>();
            ArrayList<String> methodArr = new ArrayList<>();
            ArrayList<String> errArr = new ArrayList<>();
            switch ((String) req.get("type")) {
                case "hook":
                    methods = ((JSONArray) req.get("methods")).toArray(methods);
                    for (String mtd:methods) {
                        try {
                            Method m = MethodHandler.getMethodbyJavaName(mtd);
                            if (!unhooks.containsKey(mtd))
                                unhooks.put(mtd, HookHandler.getProvider().hookMethod(m, hook));
                        }catch (Throwable e){
                            e.printStackTrace();
                            errArr.add(mtd+":"+e.getMessage());
                        }
                    }
                    call.put("type","hook");
                    call.put("errors",errArr);
                    call.put("hooks",unhooks.keySet());
                    break;
                case "hookClass":
                    classes = ((JSONArray) req.get("classes")).toArray(methods);
                    for (String clzname:classes) {
                        try {
                            Class clz = Class.forName(clzname, false, XServer.classLoader);
                            for (Method m : clz.getDeclaredMethods()) {
                                String mtd = Utils.getJavaName(m);
                                if (!unhooks.containsKey(mtd))
                                    unhooks.put(mtd, HookHandler.getProvider().hookMethod(m, hook));
                            }
                        }catch (Throwable e){
                            e.printStackTrace();
                            errArr.add(clzname+":"+e.getMessage());
                        }
                    }
                    call.put("type","hook");
                    call.put("errors",errArr);
                    call.put("hooks",unhooks.keySet());
                    break;
                case "unhook":
                    methods = ((JSONArray) req.get("methods")).toArray(methods);
                    for (String mtd:methods) {
                        if(unhooks.containsKey(mtd))unhooks.remove(mtd).unhook();
                    }
                    call.put("type","unhook");
                    call.put("hooks",unhooks.keySet());
                    break;
                case "classes":
                    call.put("type","classes");
                    call.put("classes",DexHelper.getClassesInDex(XServer.classLoader));
                    break;
                case "methods":
                    classes = ((JSONArray) req.get("classes")).toArray(classes);
                    for (String clz:classes) {
                        try{
                            for (Method method:Class.forName(clz,false,XServer.classLoader).getDeclaredMethods()) {
                                methodArr.add(Utils.getJavaName(method));
                            }
                            clzArr.add(clz);
                        }catch (Throwable e){
                            e.printStackTrace();
                            errArr.add(clz+" "+e.getLocalizedMessage());
                        }
                    }
                    call.put("type","methods");
                    call.put("methods",methodArr);
                    call.put("errors",errArr);
                    call.put("classes",clzArr);
                    break;
            }
            websocket.trySend(call.toJSONString());
        }catch (Exception e){
            e.printStackTrace();
        }
        Log.e("[XServer Debug]", (String)req.get("type"));
    }

    // 包装一下就好，将显示相关的内容交给前端去处理
    private class hook extends XServer_MethodHook {
        boolean loopLockBefore = false;
        boolean loopLockAfter = false;

        @Override
        public void beforeHookedMethod(XServer_Param param) throws Throwable {
            super.beforeHookedMethod(param);
            if(loopLockBefore || loopLockAfter){
                Log.e("[XServer Debug]", "Avoiding Loop on " + Utils.getJavaName((Method) param.method));
                return;
            }
            try {
                loopLockBefore = true;
                JSONObject call = new JSONObject();
                call.put("type", "call");
                call.put("tid", Process.myTid());
                call.put("elapsed", Process.getElapsedCpuTime());
                call.put("method", Utils.getJavaName((Method) param.method));
                call.put("this", ObjectHandler.briefObject(param.thisObject));
                JSONArray params = new JSONArray();
                for (Object arg : param.args) {
                    params.add(ObjectHandler.briefObject(arg));
                }
                call.put("params", params);
                websocket.trySend(call.toJSONString());
            }finally {
                loopLockBefore = false;
            }
        }

        @Override
        public void afterHookedMethod(XServer_Param param) throws Throwable {
            super.afterHookedMethod(param);
            if(loopLockBefore || loopLockAfter){
                Log.e("[XServer Debug]", "Avoiding Loop on " + Utils.getJavaName((Method) param.method));
                return;
            }
            try {
                loopLockAfter = true;
                JSONObject result = new JSONObject();
                result.put("type","result");
                result.put("tid", Process.myTid());
                result.put("elapsed", Process.getElapsedCpuTime());
                result.put("method",Utils.getJavaName((Method) param.method));
                result.put("this", ObjectHandler.briefObject(param.thisObject));
                result.put("result",ObjectHandler.briefObject(param.getResult()));
                if(param.hasThrowable())result.put("throw",ObjectHandler.briefObject(param.getThrowable()));

                websocket.trySend(result.toJSONString());
            }finally {
                loopLockAfter = false;
            }
        }
    }

    private class ws extends NanoWSD.WebSocket {
        public ws(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }
        public void trySend(String payload) {try {send(payload);} catch (IOException e) {e.printStackTrace();}}
        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) { handleMessage(message.getTextPayload()); }
        @Override
        protected void onOpen() {
            trySend("{\"type\":\"message\",\"message\":\"XServer wsTrace Connected.\"}");
            // 返回所有类
            JSONObject classes = new JSONObject();
            classes.put("type","classes");
            classes.put("classes",ClassHandler.getAllClasses(XServer.classLoader));
        }
        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            if (unhookOnClose) {
                for (String methodname : unhooks.keySet()) {
                    unhooks.remove(methodname).unhook();
                }
            }
        }
        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {}
        @Override
        protected void onException(IOException exception) {}
    }
}
