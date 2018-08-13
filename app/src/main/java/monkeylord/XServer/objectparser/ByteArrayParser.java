package monkeylord.XServer.objectparser;

import android.util.Base64;

import monkeylord.XServer.XServer;

public class ByteArrayParser implements XServer.ObjectParser {
    @Override
    public java.lang.Object parse(java.lang.String data) {
        return Base64.decode(data, Base64.DEFAULT);
    }
}