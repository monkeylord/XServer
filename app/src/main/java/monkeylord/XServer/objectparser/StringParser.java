package monkeylord.XServer.objectparser;

import monkeylord.XServer.XServer;

public class StringParser implements XServer.ObjectParser {
    @Override
    public Object parse(java.lang.String data) {
        return data;
    }
}
