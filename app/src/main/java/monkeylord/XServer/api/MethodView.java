package monkeylord.XServer.api;

import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.MethodHandler;

public class MethodView implements XServer.Operation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            return XServer.render(
                    MethodHandler.getMethodDetail(
                            ClassHandler.findClassbyName(
                                    parms.get("class"),
                                    XposedEntry.classLoader
                            ).getDeclaredMethods()[Integer.parseInt(parms.get("method"))])
                    , "pages/methodview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
