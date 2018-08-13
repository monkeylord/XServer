package monkeylord.XServer.api;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ObjectHandler;

import static monkeylord.XServer.utils.Utils.MethodDescription;

public class MethodView implements XServer.Operation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("clzname", parms.get("class"));
            Class clz = Class.forName(parms.get("class"), false, XposedEntry.classLoader);
            map.put("method", parms.get("method"));
            Method m = clz.getDeclaredMethods()[Integer.parseInt(parms.get("method"))];
            map.put("paramList", m.getParameterTypes());
            map.put("exceptionList", m.getExceptionTypes());
            map.put("return", m.getReturnType());
            map.put("objList", ObjectHandler.objects.keySet());
            map.put("methoddes", MethodDescription(m));
            map.put("json", JSON.toJSON(m).toString());
            return XServer.render(map, "pages/methodview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
