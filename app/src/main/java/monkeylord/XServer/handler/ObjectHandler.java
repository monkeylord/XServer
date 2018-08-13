package monkeylord.XServer.handler;

import java.util.HashMap;
import java.util.Map;

public class ObjectHandler {
    public static HashMap<String, Object> objects = new HashMap<String, Object>();

    static Object storeObject(Object obj, String name) {
        return objects.put(name, obj);
    }

    static Object getObject(String name) {
        return objects.get(name);
    }

    static Object[] getObjects(String name, String type) {
        return null;
    }

    static Object removeObject(String name) {
        return objects.remove(name);
    }

    static Object removeObject(Object object) {
        for (Map.Entry entry : objects.entrySet()) {
            if (entry.getValue().equals(object)) return objects.remove(entry.getKey());
        }
        return null;
    }
}
