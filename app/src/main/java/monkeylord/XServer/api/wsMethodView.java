package monkeylord.XServer.api;

import android.os.Process;

import com.alibaba.fastjson.JSON;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.NanoWSD;
import monkeylord.XServer.utils.Utils;
import monkeylord.XServer.utils.netUtil;

import static android.R.attr.data;
import static monkeylord.XServer.XServer.parsers;

//单方法监控的WebSocket处理
public class wsMethodView implements XServer.wsOperation {
    @Override
    public NanoWSD.WebSocket handle(NanoHTTPD.IHTTPSession handshake) {
        return new ws(handshake);
    }

    public class ws extends NanoWSD.WebSocket {
        public Method m = null;
        public String server="http://127.0.0.1:8000";//TODO +Process.myPid();
        XC_MethodHook.Unhook unhook = null;
        MethodHook myHook = new MethodHook(this);
        boolean modify = true;
        HashMap<String, Object> objs = new HashMap<>();

        public ws(NanoHTTPD.IHTTPSession handshakeRequest) {
            super(handshakeRequest);
            try {
                Map<String, String> args = handshakeRequest.getParms();
                if(args.get("javaname")!=null) {
                    m = MethodHandler.getMethodbyJavaName(args.get("javaname"));
                }
                if(m==null)m = Class.forName(args.get("class"), false, XposedEntry.classLoader)
                        .getDeclaredMethods()[Integer.parseInt(args.get("method"))];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onOpen() {
            unhook = myHook.hook(m);
            if (unhook != null) try {
                sendLog("Start Monitoring</br>");
                sendUpdateObj();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            unhook.unhook();
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {

        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {

        }

        void sendLog(String Msg) throws IOException {
            Map<String, Object> object = new HashMap<String, Object>();
            object.put("op", "msg");
            object.put("msg", Msg);
            send(new JSONObject(object).toString());
        }

        void sendUpdateObj() throws IOException {
            Map<String, Object> object = new HashMap<String, Object>();
            object.put("op", "updatethis");
            object.put("data", new JSONArray(ObjectHandler.objects.keySet()));
            send(new JSONObject(object).toString());
        }
    }

    public class MethodHook extends XC_MethodHook {
        public ws myws;
        public Member method;            //被Hook的方法
        public Object thisObject;        //方法被调用时的this对象
        public Object[] args;            //方法被调用时的参数
        private Object result = null;    //方法被调用后的返回结果
        private int pid = 0;

        MethodHook(ws ws) {
            myws = ws;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }

        private void gatherInfo(MethodHookParam param) {
            method = param.method;
            thisObject = param.thisObject;
            args = param.args;
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
            if (pid > 0 && pid != Process.myPid()) return;
            gatherInfo(param);
            if(Invoke_New.isMe()){
                StringBuilder sb=new StringBuilder();
                StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
                sb.append("<details closed>");
                sb.append("<summary>Forwarding..</summary>");
                for (int i = 0; i <stacks.length ; i++) {
                    sb.append("<p>"+stacks[i].getClassName()+"."+stacks[i].getMethodName()+"</p>");
                }
                sb.append("/<details>");
                myws.sendLog(sb.toString());
                return;
            }
            if (thisObject != null) {
                ObjectHandler.objects.put(thisObject.getClass().getName() + "@" + Integer.toHexString(new Random().nextInt()), thisObject);
                myws.sendUpdateObj();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<details open>");
            sb.append("<summary>[" + Process.myPid() + "]" + method.getDeclaringClass().getName() + "." + MethodDescription(param).toString() + " Called</summary>");
            sb.append("<details>");
            sb.append("<summary>Stack Trace</summary>");
            StackTraceElement[] strace = Thread.currentThread().getStackTrace();
            for (int i = 4; i < strace.length; i++) {
                sb.append("<p> at " + strace[i].getClassName() + "." + strace[i].getMethodName() + " : " + strace[i].getLineNumber() + "</p>");
            }
            sb.append("</details>");
            sb.append("<dl>");
            try {
                if (args != null) for (int i = 0; i < args.length; i++) {
                    sb.append("<dt>Arg" + i + " " + args[i].getClass().getName() + "</dt>");
                    sb.append("<dd>" + translate(args[i]) + "</dd>");
                }
            } catch (Throwable e) {
                sb.append("<p>" + e.getLocalizedMessage() + "</p>");
            } finally {
                sb.append("</dl>");
                //sb.append("</details>");
                log(sb.toString());
            }
            if (myws.modify) {
                HashMap json=new HashMap();
                json.put("method", Utils.getJavaName(myws.m));
                if(thisObject!=null)json.put("this",ObjectHandler.saveObject(thisObject));
                ArrayList params=new ArrayList();
                if(args!=null)for (Object arg:args) {
                    params.add(ObjectHandler.saveObject(arg));
                }
                json.put("params",params.toArray());
                ArrayList stacks=new ArrayList();
                for (StackTraceElement element:Thread.currentThread().getStackTrace()) {
                    stacks.add(element.getClassName() + "." + element.getMethodName() + " : " + element.getLineNumber());
                }
                json.put("stack",stacks);
                param.setResult(
                        ObjectHandler.parseObject(
                                new netUtil(myws.server + "/invoke2", new JSONObject(json).toString(2)).getRet()
                        )
                );
            }
        }

        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            super.beforeHookedMethod(param);
            if (pid > 0 && pid != Process.myPid()) return;
            gatherInfo(param);
            if(Invoke_New.isMe())return;
            result = param.getResult();
            //Write your code here.
            if (myws.modify) {
                //param.setResult(requestResult(result));
            }
            StringBuilder sb = new StringBuilder();
            //sb.append("<details open>");
            //sb.append("<summary>["+Process.myPid() +"]"+ method.getDeclaringClass().getName() + "." + MethodDescription(param).toString() +" Returned</summary>");
            sb.append("<dl>");
            try {
                if (param.getThrowable() == null) {
                    sb.append("<dt>Return</dt><dd>" + translate(result) + "</dd>");
                    sb.append("</dl>");
                } else {
                    sb.append("<dt>Throw</dt><dd>" + translate(param.getThrowable()) + "</dd>");
                    sb.append("</dl>");
                }
            } catch (Throwable e) {
                sb.append("<p>" + e.getLocalizedMessage() + "</p>");
            } finally {
                //log("</" + method.getDeclaringClass() + " method=" + MethodDescription(param).toString() +" pid="+Process.myPid()+ ">");
                sb.append("</details>");
                log(sb.toString());
            }
        }

        private void log(String log) {
            //You can add your own logger here.
            //e.g filelogger like Xlog.log(log);
            try {
                myws.sendLog(log);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String translate(Object obj) {
            //Write your translator here.
            if (obj == null) return "null";
            if (obj.getClass().getName().equals("java.lang.String")) return obj.toString();
            else if(Utils.getTypeSignature(obj.getClass()).equals("[B"))return parsers.get("[B").generate(obj);
            //else return JSON.toJSONString(obj);
            else return parsers.get("store").generate(obj);
        }

        private String MethodDescription(MethodHookParam param) {
            StringBuilder sb = new StringBuilder();
            sb.append(method.getName().toString());
            sb.append("(");
            if (args != null) for (Object arg : args) {
                if (arg == null) sb.append("UnknownType");
                else if (arg.getClass().isPrimitive()) sb.append(arg.getClass().getSimpleName());
                else sb.append(arg.getClass().getName());
                sb.append(",");
            }
            sb.append(")");
            return sb.toString();
        }

        public Unhook hook(Member method) {
            return XposedBridge.hookMethod(method, this);
        }

        public void hook(String clzn, String methodRegEx) throws ClassNotFoundException {
            Pattern pattern = Pattern.compile(methodRegEx);
            for (Member method : Class.forName(clzn, false, XposedEntry.classLoader).getDeclaredMethods()) {
                if (pattern.matcher(method.getName()).matches() && !method.isSynthetic())
                    this.hook(method);
            }
        }
    }
}
