package monkeylord.XServer.api;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import monkeylord.XServer.XServer;
import monkeylord.XServer.XposedEntry;
import monkeylord.XServer.handler.ObjectHandler;

public class Loopnare extends XC_MethodHook implements XServer.Operation {
    public static HashMap<String, XC_MethodHook.Unhook> unhooks = new HashMap<String, Unhook>();

    @Override
    public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        try {
            Method[] methods = Class.forName(parms.get("class"), false, XposedEntry.classLoader).getDeclaredMethods();
            Method m = methods[Integer.parseInt(parms.get("method"))];
            if (!unhooks.containsKey(parms.get("class") + "." + m.getName()))
                unhooks.put(parms.get("class") + "." + m.getName(), XposedBridge.hookMethod(m, this));
            sb.append("OK");
            for (String unhook : unhooks.keySet()) {
                sb.append("<br>");
                sb.append(unhook);
            }
        } catch (Exception e) {
            sb.append(e.getLocalizedMessage());
        }
        return sb.toString();
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
        ObjectHandler.objects.put("Instance-" + param.method.getDeclaringClass().getName() + "." + param.method.getName() + "-" + param.thisObject.hashCode(), param.thisObject);
        unhooks.remove(param.method.getDeclaringClass() + "." + param.method.getName());
        XposedBridge.log(param.method.getName() + "Catched!");
    }
}
