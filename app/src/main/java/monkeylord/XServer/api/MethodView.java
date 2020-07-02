package monkeylord.XServer.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ClassHandler;
import monkeylord.XServer.handler.MethodHandler;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.Utils;

//查看方法详情页面
public class MethodView extends BaseOperation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Method method=null;
            if(parms.get("javaname")!=null) {
                method = MethodHandler.getMethodbyJavaName(parms.get("javaname"));
                for (int i = 0; i < method.getDeclaringClass().getDeclaredMethods().length; i++) {
                    if(Utils.getJavaName(method.getDeclaringClass().getDeclaredMethods()[i]).equals(parms.get("javaname"))){
                        parms.put("method",""+i);
                    }
                }
            }
            if(method==null)method=ClassHandler.findClassbyName(parms.get("class"),XServer.classLoader).getDeclaredMethods()[Integer.parseInt(parms.get("method"))];
            HashMap<String, Object> map = MethodHandler.getMethodDetail(method);
            map.put("method",parms.get("method"));
            map.put("objList", ObjectHandler.objects.keySet());
            return XServer.render(map, "pages/methodview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }
}
