package monkeylord.XServer.objectparser;

import com.alibaba.fastjson.JSONArray;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ObjectHandler;

public class CollectionParser implements XServer.ObjectParser {
    public static HashMap<String, Object> objects = new HashMap<String, Object>();
    @Override
    public Object parse(String data) {
        String name=data.substring(0,data.indexOf("=>"));
        String array=data.substring(data.indexOf("=>")+2);
        Collection collection = (Collection) objects.get(name);
        collection.clear();
        for (Object item : JSONArray.parseArray(array)) {
            collection.add(ObjectHandler.parseObject(item.toString()));
        }
        return collection;
    }

    @Override
    public String generate(Object obj) {
        if(obj instanceof Collection){
            JSONArray array = new JSONArray();
            for (Object o : ((Collection) obj)) {
                array.add(ObjectHandler.saveObject(o));
            }
            objects.put(""+obj.hashCode(),obj);
            return obj.hashCode()+"=>"+array.toJSONString();
        }else return "Error Not Collection";
    }
}
