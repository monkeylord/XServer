package monkeylord.XServer;

import android.os.Process;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import monkeylord.XServer.api.ClassView;
import monkeylord.XServer.api.Invoke;
import monkeylord.XServer.api.Invoke_New;
import monkeylord.XServer.api.MethodView;
import monkeylord.XServer.api.Tracer;
import monkeylord.XServer.api.wsMethodView;
import monkeylord.XServer.api.wsTracer;
import monkeylord.XServer.api.wsTracerNew;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.objectparser.ByteArrayParser;
import monkeylord.XServer.objectparser.IntParser;
import monkeylord.XServer.objectparser.StringParser;
import monkeylord.XServer.utils.DexHelper;
import monkeylord.XServer.utils.NanoWSD;

public class XServer extends NanoWSD {
    public static HashMap<String, ObjectParser> parsers = new HashMap<String, ObjectParser>();
    static Hashtable<String, Operation> route = new Hashtable<String, Operation>();
    static Hashtable<String, wsOperation> wsroute = new Hashtable<String, wsOperation>();

    public XServer(int port) {
        this(port, null);
    }

    public XServer(int port, Hashtable<String, Operation> route) {
        super(port);
        parsers.put("string", new StringParser());
        parsers.put("int", new IntParser());
        parsers.put("byte", new ByteArrayParser());
        parsers.put("Ljava.lang.String;", new StringParser());
        parsers.put("I", new IntParser());
        parsers.put("[B", new ByteArrayParser());

        wsroute.put("/", new wsTracer());
        wsroute.put("/methodview", new wsMethodView());
        wsroute.put("/wsTraceNew", new wsTracerNew());

        if (route != null) XServer.route = route;
        XServer.route.put("/", new index());
        XServer.route.put("/classview", new ClassView());
        XServer.route.put("/methodview", new MethodView());
        XServer.route.put("/tracer", new Tracer());
        XServer.route.put("/invoke", new Invoke());
        XServer.route.put("/invoke2", new Invoke_New());
        try {
            start(0, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        wsOperation wsop = wsroute.get(handshake.getUri());
        if (wsop != null) return wsop.handle(handshake);
        else return wsroute.get("/").handle(handshake);
    }

    @Override
    public Response serveHttp(IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        Map<String, String> headers = null;
        try {
            headers = session.getHeaders();
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String uri = session.getUri();
        Operation operation = route.get(uri.toLowerCase());
        if (operation == null) operation = route.get("/");
        return newFixedLengthResponse(operation.handle(uri, session.getParms(), headers, files));
    }
    public void Register(String uri, Operation op) {
        route.put(uri, op);
    }
    public void Register(String uri, wsOperation op) {
        wsroute.put(uri, op);
    }

    public static String render(Map<String, Object> model, String page) throws IOException, TemplateException {
        Template tmp = new Template(page, new InputStreamReader(XposedEntry.res.getAssets().open(page)), null);
        StringWriter sw = new StringWriter();
        tmp.process(model, sw);
        return sw.toString();
    }


    public interface ObjectParser {
        Object parse(String data);
        String generate(Object obj);
    }

    public interface Operation {
        String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files);
    }

    public interface wsOperation {
        WebSocket handle(IHTTPSession handshake);
    }

    public class index implements XServer.Operation {
        @Override
        public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
            try {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("opList", route.keySet());
                map.put("params", parms);
                map.put("headers", headers);
                map.put("clzs", DexHelper.getClassesInDex(XposedEntry.classLoader));
                map.put("parsers", parsers);
                map.put("objs", ObjectHandler.objects);
                map.put("pid", String.valueOf(Process.myPid()));
                return render(map, "pages/index.html");
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
        }
    }
}