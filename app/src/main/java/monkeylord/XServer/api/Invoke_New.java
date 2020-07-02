package monkeylord.XServer.api;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.NanoHTTPD;

//处理反射调用的另一个接口
public class Invoke_New implements XServer.Operation {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session){
        Map<String, String> files = new HashMap<String, String>();
        Map<String, String> headers = null;
        try {
            headers = session.getHeaders();
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String uri = session.getUri();
        StringBuilder sb = new StringBuilder();
        try {
            Log.d("XServer", "Invoke2 postData:"+files.get("postData"));
            JSONObject data = JSON.parseObject(files.get("postData"));
            Method method= MethodHandler.getMethodbyJavaName(data.getString("method"));
            //method.setAccessible(true);
            Object thisobj = (data.getString("this")!=null)?ObjectHandler.parseObject(data.getString("this")):null;
            Object[] params = new Object[method.getParameterTypes().length];
            JSONArray paramList = data.getJSONArray("params");
            for (int i = 0; i < params.length; i++) {
                params[i]=ObjectHandler.parseObject(paramList.getString(i));
                Log.d("XServer", "Invoke2 Arg"+i+":"+paramList.getString(i));
            }
            method.setAccessible(true);
            sb.append(ObjectHandler.saveObject(method.invoke(thisobj, params)));
        } catch (Exception e) {
            sb.append(e.getMessage());
            sb.append("\r\n");
            for (StackTraceElement st:e.getStackTrace()) {
                sb.append(st.toString());
                sb.append('\n');
            }
            e.printStackTrace();
            Log.i("[XServer Debug]", sb.toString() );
            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,NanoHTTPD.MIME_PLAINTEXT,ObjectHandler.saveObject(new XServerWrappedThrowable(e.getCause(),true)));
        }
        Log.d("XServer", "Invoke2 return:"+sb.toString());
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,NanoHTTPD.MIME_PLAINTEXT,sb.toString());
    }
    public static boolean isMe(){
        StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        for (int i = 4; i <stacks.length ; i++) {
            if(stacks[i].getClassName().equals(Invoke_New.class.getName()))return true;
        }
        return false;
    }
    public class XServerWrappedThrowable {
        Throwable throwable;
        String atip = "Obviously something is thrown by the method, change <shouldPassthough> to false if you want response handler ignore the throwable";
        boolean shouldPassthough;
        String message;
        String stacks;

        XServerWrappedThrowable(Throwable throwable, boolean shouldPassthough){
            this.throwable = throwable;
            this.message = throwable.toString();
            this.shouldPassthough = shouldPassthough;
            this.stacks = JSON.toJSONString(throwable.getStackTrace(),true);
        }

        XServerWrappedThrowable(Throwable throwable){
            this(throwable,false);
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
