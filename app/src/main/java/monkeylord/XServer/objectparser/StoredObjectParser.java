package monkeylord.XServer.objectparser;

import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.utils.Utils;

import static monkeylord.XServer.XServer.parsers;

public class StoredObjectParser implements XServer.ObjectParser {
    public static HashMap<String, Object> objects = new HashMap<String, Object>();
    @Override
    public Object parse(String data) {
        String objname=data.substring(0,data.indexOf("=>"));
        String fieldmapStr=data.substring(data.indexOf("=>")+2);
        Object obj = objects.get(objname);
        if(obj==null)obj = ObjectHandler.objects.get(objname);
        if(obj==null)return null;
        HashMap<String, String> fieldmap = (HashMap)JSON.parseObject(fieldmapStr,HashMap.class);
        if(!fieldmap.isEmpty()){
            try {
                Class objClass = obj.getClass();
                for (Map.Entry<String, String> entry : fieldmap.entrySet()) {
                    Field field = objClass.getDeclaredField(entry.getKey());
                    field.setAccessible(true);
                    field.set(obj,ObjectHandler.parseObject(entry.getValue()));
                }
            }catch (Exception e){
                Log.e("XServer", "SOParser: " + e.getLocalizedMessage() );
            }
        }
        return obj;
    }

    @Override
    public String generate(Object obj) {
        String objname;
        try{
            objname=""+obj.hashCode();
        }catch (Exception e){
            objname= ""+new Random().nextLong();
        }
        HashMap<String, String> fieldmap = new HashMap<String, String>();
        try{
            Field fields[] = obj.getClass().getDeclaredFields();
            for (Field field:fields) {
                // 只处理public属性
                // Handle public field only
                //if(field.isAccessible()){
                    field.setAccessible(true);
                    Object fieldObj = field.get(obj);
                    Log.i("XServer", "SOParser Field: " + field.getName() + "@" + Utils.getTypeSignature(fieldObj.getClass()));
                    // 只处理原始类型，避免循环引用
                    // Handle primitive type only, avoid cell.
                    if(fieldObj.getClass().isPrimitive()||parsers.get(Utils.getTypeSignature(fieldObj.getClass()))!=null){
                        fieldmap.put(field.getName(),ObjectHandler.saveObject(fieldObj));
                    }
                //}
            }
        }catch (Exception e){
            Log.e("XServer", "SOParser: " + e.getLocalizedMessage() );
        }
        String fieldmapStr = JSON.toJSONString(fieldmap,true);
        objects.put(""+objname,obj);
        return objname+"=>"+fieldmapStr;
    }
}
