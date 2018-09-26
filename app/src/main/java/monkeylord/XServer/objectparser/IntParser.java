package monkeylord.XServer.objectparser;

import monkeylord.XServer.XServer;

public class IntParser implements XServer.ObjectParser {
    @Override
    public java.lang.Object parse(java.lang.String data) {
        return Integer.parseInt(data);
    }

    @Override
    public String generate(Object obj) {
        return obj.toString();
    }
}