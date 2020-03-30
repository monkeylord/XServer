package monkeylord.XServer.api;

import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.utils.NanoHTTPD;

import static monkeylord.XServer.utils.NanoHTTPD.newFixedLengthResponse;

public abstract class BaseOperation implements XServer.Operation {
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session){
        Map<String, String> files = new HashMap<String, String>();
        Map<String, String> headers = null;
        try {
            headers = session.getHeaders();
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String uri = session.getUri();
        return newFixedLengthResponse(handle(uri, session.getParms(), headers, files));
    }

    abstract String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files);
}
