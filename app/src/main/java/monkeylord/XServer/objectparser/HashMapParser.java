package monkeylord.XServer.objectparser;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ObjectHandler;

import static monkeylord.XServer.XServer.parsers;

public class HashMapParser implements XServer.ObjectParser {
    public static HashMap<String, Object> objects = new HashMap<String, Object>();
    @Override
    public Object parse(String data) {
        //HashMap obj = new HashMap();
        String name=data.substring(0,data.indexOf("=>"));
        String kvs=data.substring(data.indexOf("=>")+2);
        HashMap map = (HashMap) objects.get(name);
        if(map==null)map = new HashMap<>();
        map.clear();
        for (Map.Entry<String, Object> entry : JSON.parseObject(kvs).entrySet()) {
            map.put(ObjectHandler.parseObject(entry.getKey()),ObjectHandler.parseObject(entry.getValue().toString()));
        }
        return map;
    }

    @Override
    public String generate(Object obj) {
        HashMap map = (HashMap) obj;

        JSONObject jsonObject = new JSONObject();
        for (Map.Entry entry: (Set<Map.Entry>)map.entrySet()) {
            Log.e("[XServer]", entry.getKey().toString());
            Log.e("[XServer]", entry.getValue().toString());
            jsonObject.put(ObjectHandler.saveObject(entry.getKey()),ObjectHandler.saveObject(entry.getValue()));
        }

        objects.put(""+obj.hashCode(),obj);
        Log.e("[XServer]", jsonObject.toJSONString());
        Log.e("[XServer]", jsonObject.toString());
        return obj.hashCode()+"=>"+jsonObject.toJSONString();
    }
}
