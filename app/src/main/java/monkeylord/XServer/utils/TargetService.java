package monkeylord.XServer.utils;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;

import de.robv.android.xposed.XposedBridge;
import monkeylord.XServer.MainActivity;

public class TargetService extends Service {
    public TargetService() {
        try {
            new NanoHTTPD(7999){
                @Override
                public Response serve(IHTTPSession session) {
                    SharedPreferences sp = getSharedPreferences("XServer", MODE_PRIVATE);
                    String targetApp = sp.getString("targetApp", "com.");
                    return newFixedLengthResponse(targetApp);
                }
            }.start();
            Log.d("XServer", "XServer SystemServer Started");
        }catch (Exception e){
            Log.d("XServer", "XServer SystemServer Fail");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
