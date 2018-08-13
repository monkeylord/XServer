package monkeylord.XServer.handler;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.utils.Utils;

public class MethodHandler {
    public static HashMap<String, Object> getMethodDetail(Method method) {
        HashMap<String, Object> detail = new HashMap<String, Object>();
        detail.put("name", method.getName());
        detail.put("class", method.getDeclaringClass().getName());
        detail.put("javaName", Utils.getJavaName(method));
        detail.put("parametersTypes", method.getParameterTypes());
        detail.put("returnType", method.getReturnType());
        detail.put("exceptionTypes", method.getExceptionTypes());
        detail.put("description", Utils.MethodDescription(method));
        detail.put("json", JSON.toJSON(method));
        return detail;
    }

    public static Method getMethod(String className, String methodName) {
        try {
            for (Method method : Class.forName(className, false, XposedEntry.classLoader).getMethods()) {
                if (method.getName().equals(methodName)) return method;
            }
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method[] getMethodsbyName(String methodName) {
        return null;
    }

    public static Method[] getMethodsInClasses(Class[] clzs) {
        ArrayList<Method> methods = new ArrayList<Method>();
        for (Class clazz : clzs) {
            for (Method method : clazz.getDeclaredMethods()) {
                methods.add(method);
            }
        }
        return (Method[]) methods.toArray();
    }

    static Object invokeMethod(Method method, Object thisObj, Object[] params) {
        try {
            method.setAccessible(true);
            return ObjectHandler.storeObject(method.invoke(thisObj, params), "invokeResult");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
