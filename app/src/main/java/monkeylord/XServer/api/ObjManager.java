package monkeylord.XServer.api;

import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;

//对象管理器页面
//TODO 增删改查编辑暂存的对象
public class ObjManager extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            if (parms.containsKey("type")) {
                switch (parms.get("type")) {
                    case "show":
                        break;
                    case "edit":
                        break;
                    default:
                        return "";
                }
            } else return XServer.render(map, "pages/objmgr.html");
            return null;
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
