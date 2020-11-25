package monkeylord.XServer.api;

import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;

/**
 * Created by Vove on 2020/11/22
 */
public class Script extends BaseOperation {
    @Override
    String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("filter", parms.get("filter"));
            return XServer.render(map, "pages/script.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
