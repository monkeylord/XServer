package monkeylord.XServer.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.utils.DexHelper;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.NanoWSD;
import monkeylord.XServer.utils.Utils;

public class wsTracerNew extends XC_MethodHook implements XServer.wsOperation {
    final boolean unhookOnClose = false;
    HashMap<String, Unhook> unhooks = new HashMap<>();
    wsTracerNew me = this;

    @Override
    public NanoWSD.WebSocket handle(NanoHTTPD.IHTTPSession handshake) {
        return new ws(handshake);
    }

    private class ws extends NanoWSD.WebSocket {
        public ws(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            trySend("XServer wsTrace Connected.Welcome.Current Hooked:");
            trySend(JSON.toJSONString(unhooks));
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            //收拾Hook
            if (unhookOnClose) {
                for (String methodname : unhooks.keySet()) {
                    unhooks.remove(methodname).unhook();
                }
            }
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            //消息处理
            JSONObject req = JSON.parseObject(message.getTextPayload());
            switch (req.getString("op")) {
                case "hook":
                    Method method = MethodHandler.getMethod(req.getString("method"));
                    if (method != null) {
                        unhooks.put(method.getDeclaringClass() + "." + method.getName() + Utils.getMethodSignature(method), XposedBridge.hookMethod(method, me));
                        trySend(method.getName() + "hooked");
                    }
                    break;
                case "unhook":
                    Unhook unhook = unhooks.remove(req.getString("method"));
                    if (unhook != null) {
                        unhook.unhook();
                        trySend(unhook.toString() + "unhooked");
                    }
                    break;
                case "masshook":
                    for (String classname : DexHelper.getClassesInDex(XposedEntry.classLoader)) {
                        if (classname.contains(req.getString("class"))) {
                            Class clz = ClassHandler.findClassbyName(classname, XposedEntry.classLoader);
                            if (clz != null) for (Method methodi : clz.getDeclaredMethods()) {
                                if (methodi.getName().contains(req.getString("method")))
                                    unhooks.put(methodi.getDeclaringClass() + "." + methodi.getName() + Utils.getMethodSignature(methodi), XposedBridge.hookMethod(methodi, me));
                            }
                        }
                    }
                    trySend(JSON.toJSONString(unhooks));
                    break;
                case "unhookall":
                    for (String methodname : unhooks.keySet()) {
                        unhooks.remove(methodname).unhook();
                    }
                    trySend("Done.");
                    break;
                case "list":
                    break;
                case "classes":
                    trySend(JSON.toJSONString(DexHelper.getClassesInDex(XposedEntry.classLoader)));
                    break;
                case "config":
                    break;
                default:
                    trySend(JSON.toJSONString(unhooks));
            }
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {
        }

        @Override
        protected void onException(IOException exception) {
        }

        private void trySend(String payload) {
            try {
                send(payload);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
