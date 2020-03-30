package monkeylord.XServer.api;

import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;

//MassTracer页面
public class Tracer extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("filter", parms.get("filter"));
            return XServer.render(map, "pages/tracer2.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
