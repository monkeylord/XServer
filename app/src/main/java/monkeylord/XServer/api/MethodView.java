package monkeylord.XServer.api;

import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;

public class MethodView implements XServer.Operation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            HashMap<String, Object> map = MethodHandler.getMethodDetail(
                    ClassHandler.findClassbyName(
                            parms.get("class"),
                            XposedEntry.classLoader
                    ).getDeclaredMethods()[Integer.parseInt(parms.get("method"))]);
            map.put("method",parms.get("method"));
            map.put("objList", ObjectHandler.objects.keySet());
            return XServer.render(map, "pages/methodview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
