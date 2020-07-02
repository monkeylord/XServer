package monkeylord.XServer.api;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.utils.DexHelper;

//类详情查看页面
public class ClassView extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            if(parms.get("op")!=null) {
                JSONObject res = new JSONObject();
                switch (parms.get("op")){
                    case "getclass":
                        res.put("class",
                                ClassHandler.getClassDetail(ClassHandler.findClassbyName(parms.get("class"), XServer.classLoader)));
                        return res.toJSONString();
                    case "getclasses":
                        res.put("classes",
                                DexHelper.getClassesInDex(XServer.classLoader));
                        return res.toJSONString();
                    default:
                        return "";
                }
            }else{
                return XServer.render(
                        ClassHandler.getClassDetail(
                                ClassHandler.findClassbyName(parms.get("class"), XServer.classLoader)),
                        "pages/classview.html");
            }
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
