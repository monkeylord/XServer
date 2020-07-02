package monkeylord.XServer.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by secneo on 2017/3/6.
 */
public class netUtil extends Thread {
    URL url;
    String postData;
    String ret;
    String error;
    List<Proxy> proxys;
    int status;

    public netUtil(String URL, String data) {
        try {
            postData = new String(data);
            ret = new String(postData);
            url = new URL(URL);
            error = null;
            proxys = ProxySelector.getDefault().select(new URI("http://www.baidu.com"));
        } catch (MalformedURLException e) {
            error = "MalformedURLException";
        } catch (URISyntaxException e) {
            error = "URISyntaxException";
        }

    }
    public netUtil(String URL, String data,List<Proxy> proxies) {
        this(URL,data);
        if(proxies==null)proxys=new ArrayList<Proxy>();
        else proxys=proxies;
    }

    public void run() {
        doPost();
    }

    public String getRet() {
        try {
            start();
            join();
        } catch (InterruptedException e) {
            error = "InterruptedException";
        }
        return ret;
    }

    public boolean isError() {
        if (error.equals("")) return true;
        else return false;
    }

    public String getError() {
        return error;
    }

    private void doPost() {
        try {
            //proxys.add(new Proxy(Proxy.Type.HTTP,new InetSocketAddress("127.0.0.1",8080)));
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection((proxys.size()>0)?proxys.get(0):Proxy.NO_PROXY);
            httpURLConnection.setRequestMethod("POST");// 提交模式
            httpURLConnection.setRequestProperty("Content-Type","application/json; charset=utf-8");
            httpURLConnection.setConnectTimeout(10000);//连接超时 单位毫秒
            //httpURLConnection.setReadTimeout(2000);//读取超时 单位毫秒
            // 发送POST请求必须设置如下两行
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            PrintWriter printWriter = new PrintWriter(httpURLConnection.getOutputStream());
            // 发送请求参数
            printWriter.write(postData);//post的参数 xx=xx&yy=yy
            // flush输出流的缓冲
            printWriter.flush();
            //开始获取数据
            status = httpURLConnection.getResponseCode();
            BufferedInputStream bis = new BufferedInputStream((httpURLConnection.getResponseCode()!=200)?httpURLConnection.getErrorStream():httpURLConnection.getInputStream());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len;
            byte[] arr = new byte[1024];
            while ((len = bis.read(arr)) != -1) {
                bos.write(arr, 0, len);
                bos.flush();
            }
            bos.close();
            ret = bos.toString();
        } catch (Exception e) {
            error = "Exception";
        }
    }
}