package monkeylord.XServer.objectparser;

import monkeylord.XServer.XServer;
import monkeylord.XServer.handler.ObjectHandler;

public class StoredObjectParser implements XServer.ObjectParser {
    @Override
    public Object parse(String data) {
        return ObjectHandler.objects.get(data);
    }

    @Override
    public String generate(Object obj) {
        return null;
    }
}
