package monkeylord.XServer.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import monkeylord.XServer.XServer;
import monkeylord.XServer.utils.NanoHTTPD;

import static monkeylord.XServer.utils.NanoHTTPD.newChunkedResponse;
import static monkeylord.XServer.utils.NanoHTTPD.newFixedLengthResponse;

public class FileAccess implements XServer.Operation {
    @Override
    public NanoHTTPD.Response handle(NanoHTTPD.IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(session.getParms().get("op")!=null) {
            try {
                File file = new File(session.getParms().get("path"));
                switch (session.getParms().get("op")) {
                    case "mkdir":
                        if(file.mkdirs())return newFixedLengthResponse("Done");
                        else throw new Exception("Fail to mkdir.");
                    case "readdir":
                        HashMap<String,JSONObject> fileList = new HashMap<>();
                        for (File listfile:file.listFiles()) {
                            JSONObject fileObj = new JSONObject();
                            fileObj.put("isDirectory",listfile.isDirectory());
                            fileObj.put("lastModified",listfile.lastModified());
                            fileObj.put("length",listfile.length());
                            fileObj.put("canRead",listfile.canRead());
                            fileObj.put("canWrite",listfile.canWrite());
                            fileObj.put("canExecute",listfile.canExecute());
                            fileList.put(listfile.getName(),fileObj);
                        }
                        if(fileList==null)throw new Exception("Cannot access directory");
                        return newFixedLengthResponse(JSON.toJSONString(fileList));
                    case "read":
                        //if(!file.canRead())file.setReadable(true);
                        if(!file.canRead())throw new Exception("Cannot access file");
                        FileInputStream fis = new FileInputStream(file);
                        NanoHTTPD.Response res = newChunkedResponse(NanoHTTPD.Response.Status.OK,"application/octet–stream",fis);
                        res.addHeader("Content-Disposition","attachment;filename="+file.getName());
                        return res;
                    case "write":
                        FileWriter fw = new FileWriter(file);
                        fw.write(files.get("postData"));
                        fw.close();
                        return newFixedLengthResponse("Done");
                    case "delete":
                        if(file.delete())return newFixedLengthResponse("Done");
                        else throw new Exception("Fail to delete.");
                    default:
                        return newFixedLengthResponse("Unknown Operation");
                }
            }catch (Exception e){
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getLocalizedMessage());
            }
        }else{
            HashMap<String, Object> map = new HashMap<>();
            String appName =  XServer.currentApp;
            map.put("dir", (appName!=null)?"/data/data/"+ appName: "/");
            try {
                return newFixedLengthResponse(XServer.render(map, "pages/filemgr.html"));
            } catch (Exception e) {
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getLocalizedMessage());
            }
        }
    }
}
