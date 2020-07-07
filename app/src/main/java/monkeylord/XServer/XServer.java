package monkeylord.XServer;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import monkeylord.XServer.api.ClassView;
import monkeylord.XServer.api.FileAccess;
import monkeylord.XServer.api.Invoke;
import monkeylord.XServer.api.Invoke_New;
import monkeylord.XServer.api.MemoryView;
import monkeylord.XServer.api.MethodView;
import monkeylord.XServer.api.Tracer;
import monkeylord.XServer.api.wsMethodView;
import monkeylord.XServer.api.wsMethodViewNew;
import monkeylord.XServer.api.wsTracer;
import monkeylord.XServer.api.wsTracerNew;
import monkeylord.XServer.handler.ObjectHandler;
import monkeylord.XServer.objectparser.BooleanParser;
import monkeylord.XServer.objectparser.ByteArrayParser;
import monkeylord.XServer.objectparser.GenericParser;
import monkeylord.XServer.objectparser.IntParser;
import monkeylord.XServer.objectparser.StoredObjectParser;
import monkeylord.XServer.objectparser.StringParser;
import monkeylord.XServer.utils.DexHelper;
import monkeylord.XServer.utils.NanoHTTPD;
import monkeylord.XServer.utils.NanoWSD;

public class XServer extends NanoWSD {
    public static HashMap<String, ObjectParser> parsers = new HashMap<String, ObjectParser>();
    static Hashtable<String, Operation> route = new Hashtable<String, Operation>();
    static Hashtable<String, wsOperation> wsroute = new Hashtable<String, wsOperation>();
    public static ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    public static String currentApp = "";
    public static AssetManager assetManager;

    public XServer(int port) {
        this(port, null);
    }

    public XServer(int port, Hashtable<String, Operation> route) {
        super(port);
        //确定应用名称
        if(currentApp=="") {
            try {
                currentApp = (String) Class.forName("android.app.ActivityThread").getDeclaredMethod("currentPackageName").invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //注册对象序列化/反序列化处理器
        parsers.put("store", new StoredObjectParser());
        parsers.put("generic", new GenericParser());
        parsers.put("string", new StringParser());
        parsers.put("int", new IntParser());
        parsers.put("Ljava.lang.Integer;", new IntParser());
        parsers.put("Ljava.lang.Boolean;", new BooleanParser());
        parsers.put("Ljava.lang.String;", new StringParser());
        parsers.put("I", new IntParser());
        parsers.put("Z", new BooleanParser());
        parsers.put("byte", new ByteArrayParser());
        parsers.put("[B", new ByteArrayParser());

        //注册WebSocket路由
        //wsroute.put("/", new wsTracer());
        //wsroute.put("/methodview", new wsMethodView());
        wsroute.put("/methodview2", new wsMethodViewNew());
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
        XServer.route.put("/file", new FileAccess());
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
        /*
        //先做一下基本解析
        Map<String, String> files = new HashMap<String, String>();
        Map<String, String> headers = null;
        try {
            headers = session.getHeaders();
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        //处理路由
        String uri = session.getUri();
        Operation operation = route.get(uri.toLowerCase());
        if (operation == null)try{
            assetManager.open(uri.substring(1));
            operation = new assets();
        }catch (IOException e){
            operation = route.get("/");
        }
        //return newFixedLengthResponse(operation.handle(uri, session.getParms(), headers, files));
        return operation.handle(session);
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
        Template tmp = new Template(page, new InputStreamReader(assetManager.open(page)), null);
        StringWriter sw = new StringWriter();
        tmp.process(model, sw);
        return sw.toString();
    }
    public static String file(String page) throws IOException, TemplateException {
        InputStreamReader reader = new InputStreamReader(assetManager.open(page));
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
        Response handle(IHTTPSession session);
        //String handle(String url, Map<String, String> parms, Map<String, String> headers, Map<String, String> files);
    }
    //定义WebSocket处理器
    public interface wsOperation {
        WebSocket handle(IHTTPSession handshake);
    }
    //默认主页（以及调用模板引擎的示例）
    public class index implements XServer.Operation {
        @Override
        public Response handle(IHTTPSession session) {
            try {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("opList", route.keySet());
                map.put("params", session.getParms());
                map.put("headers", session.getHeaders());
                map.put("clzs", DexHelper.getClassesInDex(XServer.classLoader));
                map.put("parsers", parsers);
                map.put("objs", ObjectHandler.objects);
                map.put("pid", String.valueOf(Process.myPid()));
                return newFixedLengthResponse(render(map, "pages/index.html"));
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getLocalizedMessage());
            }
        }
    }
    // 资源文件
    public class assets implements XServer.Operation {
        @Override
        public Response handle(IHTTPSession session) {
            try {
                Map<String, Object> map = new HashMap<String, Object>();
                return newFixedLengthResponse(file(session.getUri().substring(1)));
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getLocalizedMessage());
            }
        }
    }
}