package monkeylord.XServer.objectparser;

import monkeylord.XServer.XServer;

public class GenericParser implements XServer.ObjectParser {
    @Override
    public java.lang.Object parse(java.lang.String data) {
        //无法还原
        return null;
    }

    @Override
    public String generate(Object obj) {
        return obj.toString();
    }
}