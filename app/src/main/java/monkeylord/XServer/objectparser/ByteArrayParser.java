package monkeylord.XServer.objectparser;

import android.util.Base64;
import android.util.Log;

import java.util.Arrays;

import monkeylord.XServer.XServer;

public class ByteArrayParser implements XServer.ObjectParser {
    @Override
    public java.lang.Object parse(java.lang.String data) {
        if(data.indexOf('#')<0){
            return Base64.decode(data, Base64.DEFAULT);
        }else{
            String type=data.substring(0,data.indexOf("#"));
            String raw=data.substring(data.indexOf("#")+1);
            if(type.equals("raw"))return raw.getBytes();
            else return Base64.decode(raw, Base64.DEFAULT);
        }
    }
    @Override
    public String generate(Object obj) {
        Log.d("ByteArray", ""+new String((byte[])obj).getBytes().length+":"+((byte[])obj).length);
        if(Arrays.equals((byte[])obj,new String((byte[])obj).getBytes()))return "raw#"+new String((byte[])obj);
        else return "base64#"+Base64.encodeToString((byte[])obj,Base64.DEFAULT);
    }
}