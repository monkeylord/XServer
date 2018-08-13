package monkeylord.XServer.handler;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;

import monkeylord.XServer.utils.DexHelper;

public class ClassHandler {
    public static String[] getAllClasses(ClassLoader classLoader) {
        return DexHelper.getClassesInDex(classLoader);
    }

    public static HashMap<String, Object> getClassDetail(Class clz) {
        HashMap<String, Object> detail = new HashMap<String, Object>();
        detail.put("Name", clz.getName());
        detail.put("Methods", clz.getDeclaredMethods());
        detail.put("Fields", clz.getDeclaredFields());
        detail.put("JSON", JSON.toJSON(clz));
        return detail;
    }

    public static Class findClassbyName(String clzName, ClassLoader classLoader) {
        try {
            return Class.forName(clzName, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
