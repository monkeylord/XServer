package monkeylord.XServer.api;

import java.lang.reflect.Method;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ObjectHandler;

//处理反射调用
public class Invoke extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        Object thisobj;
        Object[] params;
        thisobj = parms.get("thisobj").equals("null") ? null : ObjectHandler.objects.get(parms.get("thisobj"));

        try {
            Method[] methods = Class.forName(parms.get("class"), false, XServer.classLoader).getDeclaredMethods();
            Method m = methods[Integer.parseInt(parms.get("method"))];
            params = new Object[m.getParameterTypes().length];
            for (int i = 0; i < m.getParameterTypes().length; i++) {
                params[i] = XServer.parsers.get(parms.get("parser" + i)).parse(parms.get("param" + i));
            }
            m.setAccessible(true);
            sb.append(m.invoke(thisobj, params));
        } catch (Exception e) {
            sb.append(e.getLocalizedMessage());
        }
        return sb.toString();
    }


}
