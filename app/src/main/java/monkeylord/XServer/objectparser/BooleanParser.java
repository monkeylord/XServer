package monkeylord.XServer.objectparser;

import monkeylord.XServer.XServer;

public class BooleanParser implements XServer.ObjectParser{

    @Override
    public Object parse(String data) {
        return Boolean.parseBoolean(data);
    }

    @Override
    public String generate(Object obj) {
        return obj.toString();
    }
}
