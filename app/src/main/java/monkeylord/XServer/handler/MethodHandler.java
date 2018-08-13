package monkeylord.XServer.handler;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class MethodHandler {
    public static Object getMethodDetail(Method method) {
        HashMap<String, Object> detail = new HashMap<String, Object>();
        detail.put("Name", method.getName());
        detail.put("JavaName", "");
        detail.put("ParametersTypes", method.getParameterTypes());
        detail.put("ReturnType", method.getReturnType());
        detail.put("ExceptionTypes", method.getExceptionTypes());
        detail.put("JSON", JSON.toJSON(method));
        return detail;
    }

    public static Method getMethod(String methodName) {
        return null;
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
