package monkeylord.XServer.api;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;

import static monkeylord.XServer.utils.Utils.FieldDescription;
import static monkeylord.XServer.utils.Utils.MethodDescription;

public class ClassView implements XServer.Operation {
    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        try {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("clzname", parms.get("class"));
            Class clz = Class.forName(parms.get("class"), false, XposedEntry.classLoader);
            if (clz.getFields().length > 0) {
                ArrayList<String> fielddes = new ArrayList<>();
                for (Field field : clz.getFields()) {
                    fielddes.add(FieldDescription(field));
                }
                map.put("fieldList", fielddes);
            }
            if (clz.getDeclaredMethods().length > 0) {
                ArrayList<String> mm = new ArrayList<>();
                Method[] methods = clz.getDeclaredMethods();
                for (int i = 0; i < methods.length; i++) {
                    mm.add(MethodDescription(methods[i]));
                }
                map.put("methodList", mm);
            }
            return XServer.render(map, "pages/classview.html");
        } catch (Exception e) {
            return e.getLocalizedMessage();
        }
    }


}
