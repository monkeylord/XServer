package monkeylord.XServer.api;

import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ClassHandler;

//类详情查看页面
public class ClassView extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            return XServer.render(
                    ClassHandler.getClassDetail(
                            ClassHandler.findClassbyName(parms.get("class"), XposedEntry.classLoader)),
                    "pages/classview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
