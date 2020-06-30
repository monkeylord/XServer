package monkeylord.XServer.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import monkeylord.XServer.XServer;
import monkeylord.XServer.utils.Utils;

//处理方法相关内容
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
        detail.put("json", JSON.toJSONString(method, SerializerFeature.PrettyFormat));
        return detail;
    }

    public static Method getMethod(String className, String methodName) {
        try {
            for (Method method : Class.forName(className, false, XServer.classLoader).getMethods()) {
                if (method.getName().equals(methodName)) return method;
            }
            return null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Method getMethodbyJavaName(String javaName) {
        String classname=javaName.substring(0,javaName.indexOf("->"));
        String method=javaName.substring(javaName.indexOf("->")+2);
        Class clz=ClassHandler.findClassbyJavaName(classname,XServer.classLoader);
        if(clz==null)return null;
        for (Method m:clz.getDeclaredMethods()) {
            if(Utils.getJavaName(m).equals(javaName))return m;
        }
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
