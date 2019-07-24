package monkeylord.XServer.handler;

import java.io.Console;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import monkeylord.XServer.XServer;
import monkeylord.XServer.objectparser.GenericParser;
import monkeylord.XServer.objectparser.StoredObjectParser;
import monkeylord.XServer.utils.Utils;

import static monkeylord.XServer.XServer.parsers;

//处理对象相关内容
public class ObjectHandler {
    public static HashMap<String, Object> objects = new HashMap<String, Object>();

    public static Object storeObject(Object obj, String name) {
        return objects.put(name, obj);
    }

    public static String saveObject(Object obj){
        if(obj==null)return "Null";
        XServer.ObjectParser parser = parsers.get(Utils.getTypeSignature(obj.getClass()));
        if(parser==null)parser=parsers.get("store");
        return Utils.getTypeSignature(obj.getClass())+"#"+parser.generate(obj);
    }
    public static String briefObject(Object obj){
        if(obj==null)return "Null";
        XServer.ObjectParser parser = parsers.get(Utils.getTypeSignature(obj.getClass()));
        if(parser==null)parser=parsers.get("generic");
        return Utils.getTypeSignature(obj.getClass())+"#"+parser.generate(obj);
    }

    public static Object getObject(String name) {
        return objects.get(name);
    }

    public static Object parseObject(String Object){
        if(Object.equals("Null"))return null;
        if(Object==null)return null;
        if(Object.indexOf("#")<0)return null;
        String type=Object.substring(0,Object.indexOf("#"));
        String raw=Object.substring(Object.indexOf("#")+1);
        XServer.ObjectParser parser = parsers.get(type);
        if(parser==null)parser=parsers.get("store");
        return parser.parse(raw);
    }

    public static Object[] getObjects(String name, String type) {
        return null;
    }

    public static Object removeObject(String name) {
        return objects.remove(name);
    }

    public static Object removeObject(Object object) {
        for (Map.Entry entry : objects.entrySet()) {
            if (entry.getValue().equals(object)) return objects.remove(entry.getKey());
        }
        return null;
    }
}
