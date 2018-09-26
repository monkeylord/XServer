package monkeylord.XServer.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;

public class Invoke_New implements XServer.Operation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject data = JSON.parseObject(files.get("postData"));
            Method method= MethodHandler.getMethodbyJavaName(data.getString("method"));
            method.setAccessible(true);
            Object thisobj = ObjectHandler.parseObject(data.getString("this"));
            Object[] params = new Object[method.getParameterTypes().length];
            JSONArray paramList = data.getJSONArray("params");
            for (int i = 0; i < params.length; i++) {
                params[i]=ObjectHandler.parseObject(paramList.getString(i));
            }
            sb.append(ObjectHandler.saveObject(method.invoke(thisobj, params)));
        } catch (Exception e) {
            sb.append(e.getLocalizedMessage());
            e.printStackTrace();
        }
        return sb.toString();
    }
    public static boolean isMe(){
        StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
        for (int i = 3; i <stacks.length ; i++) {
            if(stacks[i].getClassName().equals(Invoke_New.class.getName()))return true;
        }
        return false;
    }


}
