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

    public static Class findClassbyJavaNameEx(String javaName, ClassLoader classLoader) {
        try {
            String clzName = javaName.replace('/', '.').replace(";", "");
            clzName = (clzName.charAt(0) == 'L') ? clzName.substring(1) : clzName;
            return Class.forName(clzName, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /*
    * copied from libcore.reflect.InternalNames
    * */
    public static java.lang.Class<?> findClassbyJavaName(java.lang.String internalName,java.lang.ClassLoader classLoader) {
        if (internalName.startsWith("[")) {
            return java.lang.reflect.Array.newInstance(findClassbyJavaName(internalName.substring(1), classLoader), 0).getClass();
        }
        if (internalName.equals("Z")) {
            return java.lang.Boolean.TYPE;
        }
        if (internalName.equals("B")) {
            return java.lang.Byte.TYPE;
        }
        if (internalName.equals("S")) {
            return java.lang.Short.TYPE;
        }
        if (internalName.equals("I")) {
            return java.lang.Integer.TYPE;
        }
        if (internalName.equals("J")) {
            return java.lang.Long.TYPE;
        }
        if (internalName.equals("F")) {
            return java.lang.Float.TYPE;
        }
        if (internalName.equals("D")) {
            return java.lang.Double.TYPE;
        }
        if (internalName.equals("C")) {
            return java.lang.Character.TYPE;
        }
        if (internalName.equals("V")) {
            return java.lang.Void.TYPE;
        }
        java.lang.String name = internalName.substring(1, internalName.length() - 1).replace('/', '.');
        try {
            return classLoader.loadClass(name);
        } catch (java.lang.ClassNotFoundException e) {
            java.lang.NoClassDefFoundError error = new java.lang.NoClassDefFoundError(name);
            error.initCause(e);
            throw error;
        }
    }

}
