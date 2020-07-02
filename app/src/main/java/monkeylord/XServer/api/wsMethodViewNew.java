package monkeylord.XServer.api;

import android.os.Process;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.Hook.Unhook;
import monkeylord.XServer.handler.Hook.XServer_MethodHook;
import monkeylord.XServer.handler.Hook.XServer_Param;
import monkeylord.XServer.handler.HookHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.NanoWSD;
import monkeylord.XServer.utils.Utils;
import monkeylord.XServer.utils.netUtil;

//单方法监控的WebSocket处理
public class wsMethodViewNew implements XServer.wsOperation {

    final boolean unhookOnClose = true;
    //HashMap<String, XC_MethodHook.Unhook> unhooks = new HashMap<>();
    HashMap<MethodHook, ws> wss = new HashMap<>();
    HashMap<ws, Method> methods = new HashMap<>();
    //ws websocket;
    //MethodHook hook;
    //public Method m = null;
    public String server="http://127.0.0.1:8000";//TODO +Process.myPid();

    @Override
    public NanoWSD.WebSocket handle(NanoHTTPD.IHTTPSession handshake) {
        return new ws(handshake);
    }

    public Object wsInvoke(Object[] params) throws InvocationTargetException, IllegalAccessException {
        return methods.get(this).invoke(params);
    }

    public class ws extends NanoWSD.WebSocket {

        Unhook unhook = null;
        boolean modify = true;
        HashMap<String, Object> objs = new HashMap<>();

        public ws(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
            Method m = MethodHandler.getMethodbyJavaName((handshakeRequest.getParms().get("javaname")));
            methods.put(this,m);
        }

        public void trySend(String payload) {try {send(payload);} catch (IOException e) {e.printStackTrace();}}

        @Override
        protected void onOpen() {
            Method m = methods.get(this);
            MethodHook hook = new MethodHook(m);
            wss.put(hook,this);
            JSONObject res = new JSONObject();
            res.put("type","hook");
            res.put("method", Utils.getJavaName(m));
            res.put("ishooked", unhook!=null);
            res.put("relatedObjects",new JSONArray());
            trySend(res.toJSONString());
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            //  TODO: should unhook on close
        }
        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {}
        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {}
        @Override
        protected void onException(IOException exception) {}
    }

    public class MethodHook extends XServer_MethodHook {
        public Member method;            //被Hook的方法
        public Object thisObject;        //方法被调用时的this对象
        public Object[] args;            //方法被调用时的参数
        private Object result = null;    //方法被调用后的返回结果
        private int tid = 0;

        MethodHook(Method method) { HookHandler.getProvider().hookMethod(method, this); }

        public void setTid(int tid) {
            this.tid = tid;
        }

        private void gatherInfo(XServer_Param param) {
            method = param.method;
            thisObject = param.thisObject;
            args = param.args;
        }

        public void beforeHookedMethod(XServer_Param param) throws Throwable {
            super.beforeHookedMethod(param);
            if (tid > 0 && tid != Process.myPid()) return;
            gatherInfo(param);

            // 如果是自己调用的路过的信息，就忽略
            if(Invoke_New.isMe())return;

            // 先把信息通过WebSocket发送出去
            // 详细信息去ObjectManager查
            Method m = methods.get(wss.get(this));
            JSONObject res = new JSONObject();
            JSONArray params = new JSONArray();
            for (Object arg:param.args) { params.add(ObjectHandler.saveObject(arg)); }
            res.put("type","before");
            res.put("method", Utils.getJavaName(m));
            res.put("this", ObjectHandler.saveObject(param.thisObject));
            res.put("params",params);
            res.put("stack", Thread.currentThread().getStackTrace());
            res.put("isproxy",Invoke_New.isMe());
            res.put("threadid", Process.myTid());
            wss.get(this).trySend(res.toJSONString());
            
            if (wss.get(this).modify) {
                JSONObject call = new JSONObject();

                call.put("method", Utils.getJavaName(m));
                if(thisObject!=null)call.put("this",ObjectHandler.saveObject(thisObject));
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

        public void afterHookedMethod(XServer_Param param) throws Throwable {
            super.beforeHookedMethod(param);
            if (tid > 0 && tid != Process.myPid()) return;
            gatherInfo(param);
            // 如果是自己调用的路过的信息，就忽略
            if(Invoke_New.isMe())return;
            Method m = methods.get(wss.get(this));
            result = param.getResult();
            JSONObject res = new JSONObject();
            res.put("type","after");
            res.put("method", Utils.getJavaName(m));
            res.put("isproxy",Invoke_New.isMe());
            res.put("threadid", Process.myTid());

            if(param.getThrowable() == null){
                res.put("result", ObjectHandler.saveObject(param.getResult()));
            }else {
                res.put("throws",ObjectHandler.briefObject(param.getThrowable()));
            }

            wss.get(this).trySend(res.toJSONString());
        }
    }
}
