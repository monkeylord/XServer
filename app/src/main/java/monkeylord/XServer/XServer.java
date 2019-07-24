package monkeylord.XServer;

import android.os.Process;

import java.io.BufferedReader;
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
import monkeylord.XServer.api.MemoryView;
import monkeylord.XServer.api.MethodView;
import monkeylord.XServer.api.Tracer;
import monkeylord.XServer.api.wsMethodView;
import monkeylord.XServer.api.wsTracer;
import monkeylord.XServer.api.wsTracerNew;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.objectparser.ByteArrayParser;
import monkeylord.XServer.objectparser.GenericParser;
import monkeylord.XServer.objectparser.IntParser;
import monkeylord.XServer.objectparser.StoredObjectParser;
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
        //注册对象序列化/反序列化处理器
        parsers.put("store", new StoredObjectParser());
        parsers.put("generic", new GenericParser());
        parsers.put("string", new StringParser());
        parsers.put("int", new IntParser());
        parsers.put("Ljava.lang.Integer;", new IntParser());
        parsers.put("byte", new ByteArrayParser());
        parsers.put("Ljava.lang.String;", new StringParser());
        parsers.put("I", new IntParser());
        parsers.put("[B", new ByteArrayParser());

        //注册WebSocket路由
        wsroute.put("/", new wsTracer());
        wsroute.put("/methodview", new wsMethodView());
        wsroute.put("/wsTraceNew", new wsTracerNew());
        //注册HTTP请求路由
        if (route != null) XServer.route = route;
        XServer.route.put("/", new index());
        XServer.route.put("/classview", new ClassView());
        XServer.route.put("/methodview", new MethodView());
        XServer.route.put("/tracer", new Tracer());
        XServer.route.put("/invoke", new Invoke());
        XServer.route.put("/invoke2", new Invoke_New());
        XServer.route.put("/memory", new MemoryView());
        try {
            //启动监听
            start(0, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        //处理WebSocket路由
        wsOperation wsop = wsroute.get(handshake.getUri());
        if (wsop != null) return wsop.handle(handshake);
        else return wsroute.get("/").handle(handshake);
    }

    @Override
    public Response serveHttp(IHTTPSession session) {
        //处理HTTP请求路由
        //先做一下基本解析
        Map<String, String> files = new HashMap<String, String>();
        Map<String, String> headers = null;
        try {
            headers = session.getHeaders();
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String uri = session.getUri();
        //处理路由
        Operation operation = route.get(uri.toLowerCase());
        if (operation == null)try{
            XposedEntry.res.getAssets().open(uri.substring(1));
            operation = new assets();
        }catch (IOException e){
            operation = route.get("/");
        }
        return newFixedLengthResponse(operation.handle(uri, session.getParms(), headers, files));
    }
    //供动态注册路由使用
    public void Register(String uri, Operation op) {
        route.put(uri, op);
    }
    public void Register(String uri, wsOperation op) {
        wsroute.put(uri, op);
    }

    //简单的模板引擎
    public static String render(Map<String, Object> model, String page) throws IOException, TemplateException {
        Template tmp = new Template(page, new InputStreamReader(XposedEntry.res.getAssets().open(page)), null);
        StringWriter sw = new StringWriter();
        tmp.process(model, sw);
        return sw.toString();
    }
    public static String file(String page) throws IOException, TemplateException {
        InputStreamReader reader = new InputStreamReader(XposedEntry.res.getAssets().open(page));
        int ch;
        StringWriter sw = new StringWriter();
        while ((ch = reader.read())!=-1){
            sw.write(ch);
        }
        return sw.toString();
    }

    //定义序列化/反序列化器
    public interface ObjectParser {
        Object parse(String data);
        String generate(Object obj);
    }
    //定义HTTP请求处理器
    public interface Operation {
        String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files);
    }
    //定义WebSocket处理器
    public interface wsOperation {
        WebSocket handle(IHTTPSession handshake);
    }
    //默认主页（以及调用模板引擎的示例）
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
    // 资源文件
    public class assets implements XServer.Operation {
        @Override
        public String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files) {
            try {
                Map<String, Object> map = new HashMap<String, Object>();
                return file(url.substring(1));
            } catch (Exception e) {
                return e.getLocalizedMessage();
            }
        }
    }
}