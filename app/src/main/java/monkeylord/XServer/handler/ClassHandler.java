package monkeylord.XServer.handler;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import monkeylord.XServer.utils.DexHelper;
import monkeylord.XServer.utils.Utils;

//处理类相关的内容
public class ClassHandler {
    public static String[] getAllClasses(ClassLoader classLoader) {
        return DexHelper.getClassesInDex(classLoader);
    }

    public static HashMap<String, Object> getClassDetail(Class clz) {
        HashMap<String, Object> detail = new HashMap<String, Object>();
        if (clz == null) return detail;
        detail.put("name", clz.getName());
        detail.put("methods", clz.getDeclaredMethods());
        detail.put("fields", clz.getDeclaredFields());
        detail.put("json", JSON.toJSON(clz));
        ArrayList<String> methodDescriptions = new ArrayList<String>();
        ArrayList<String> fieldDescriptions = new ArrayList<String>();
        for (Method method : clz.getDeclaredMethods()) {
            methodDescriptions.add(Utils.MethodDescription(method));
        }
        for (Field field : clz.getFields()) {
            fieldDescriptions.add(Utils.FieldDescription(field));
        }
        detail.put("methodList", methodDescriptions);
        detail.put("fieldList", fieldDescriptions);

        return detail;
    }

    public static Class findClassbyName(String clzName, ClassLoader classLoader) {
        try {
            return Class.forName(clzName, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class findClassbyJavaName(String javaName, ClassLoader classLoader) {
        try {
            String clzName = javaName.replace('/', '.').replace(";", "");
            clzName = (clzName.charAt(0) == 'L') ? clzName.substring(1) : clzName;
            return Class.forName(clzName, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
