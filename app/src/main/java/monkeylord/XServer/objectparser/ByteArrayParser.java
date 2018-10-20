package monkeylord.XServer.objectparser;

import android.util.Base64;
import android.util.Log;

import java.nio.Buffer;
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
            else if(type.equals("base64"))return Base64.decode(raw, Base64.DEFAULT);
            else return URLDecode(raw);
        }
    }
    @Override
    public String generate(Object obj) {
        Log.d("ByteArray", ""+new String((byte[])obj).getBytes().length+":"+((byte[])obj).length);
        if(Arrays.equals((byte[])obj,new String((byte[])obj).getBytes()))return "raw#"+new String((byte[])obj);
        //else return "base64#"+Base64.encodeToString((byte[])obj,Base64.DEFAULT);
        else return "URLEncoding#"+URLEncode((byte[])obj);
    }
    private static final char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    static public String URLEncode(byte[] bytes){
        StringBuffer sb=new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            if((bytes[i]>='0'&&bytes[i]<='9')||(bytes[i]>='a'&&bytes[i]<='z')||(bytes[i]>='A'&&bytes[i]<='Z')||bytes[i]=='-'||bytes[i]=='_'||bytes[i]=='.'||bytes[i]=='~')
                sb.append((char)bytes[i]);
            else {
                sb.append('%');
                sb.append(HEX_CHAR[(256+bytes[i])/16%16]);
                sb.append(HEX_CHAR[(256+bytes[i])%16]);
            }
        }
        return sb.toString();
    }
    static public byte[] URLDecode(String str){
        byte[] org=str.getBytes();
        byte[] dst=new byte[org.length];
        byte[] fin;
        int i=0,j=0;
        for (i = 0; i < org.length; i++) {
            if(org[i]=='%'){
                dst[j]=(byte)Integer.parseInt(""+(char)org[i+1]+(char)org[i+2],16);
                j++;
                i=i+2;
            }else{
                dst[j]=org[i];
                j++;
            }
        }
        fin=new byte[j];
        for (j--; j >=0 ; j--) {
            fin[j]=dst[j];
        }
        return fin;
    }
}