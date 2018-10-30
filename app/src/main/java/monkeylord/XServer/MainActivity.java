package monkeylord.XServer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {
    SharedPreferences sp;
    String hookee;
    boolean isReg;
    TextView info;
    EditText appname;
    File sharedFile;
    //CheckBox regEx;

    private static boolean isModuleActive() {
        return false;
    }
    private File getFile(){ return null; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        super.setContentView(layout, param);
        sp = getSharedPreferences("XServer", MODE_WORLD_READABLE);
        sharedFile=getFile();
        try {
            hookee = new BufferedReader(new FileReader(sharedFile)).readLine();
        } catch (Exception e) {
            hookee = sp.getString("targetApp", "com.");
        }
        //isReg = sp.getBoolean("isReg", false);
        final AppAdapter appAdapter = new AppAdapter(this);
        final AlertDialog selector = new AlertDialog.Builder(this)
                .setTitle("Select App")
                .setAdapter(appAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        hookee = ((PackageInfo) appAdapter.getItem(i)).packageName;
                        update();
                        dialogInterface.dismiss();
                    }
                })
                .create();
        TextView welcome = new TextView(this);
        welcome.setText("XServer's App Selector");
        welcome.setTextSize(20f);
        welcome.setTextColor(Color.BLACK);
        info = new TextView(this);
        appname = new EditText(this);
        //regEx = new CheckBox(this);
        //regEx.setText("use RegEx");
        Button apply = new Button(this);
        apply.setText("Apply");
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hookee = appname.getText().toString();
                //isReg = regEx.isChecked();
                update();
            }
        });
        Button selectApp = new Button(this);
        selectApp.setText("Select App");
        selectApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selector.show();
            }
        });
        TextView tips = new TextView(this);
        tips.setText("Note:\n" +
                "XServer will listen on 2 ports when target app start: 8000 and PID \n" +
                "To connect XServer, ADB command can be used like:\n" +
                "   adb forward tcp:8000 tcp:8000\n" +
                "   adb forward tcp:8000 tcp:[PID]\n" +
                "Then you can open http://127.0.0.1:8000 in browser to interact with XServer");
        tips.setTextSize(15f);
        tips.setTextColor(Color.GRAY);
        if (!isModuleActive()) {
            TextView alert = new TextView(this);
            alert.setText("Be Awared: Module Inactive");
            alert.setTextColor(Color.RED);
            layout.addView(alert);
        }
        layout.addView(welcome);
        layout.addView(info);
        layout.addView(appname);
        //layout.addView(regEx);
        layout.addView(apply);
        layout.addView(selectApp);
        layout.addView(tips);
        update();
    }

    public void update() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("targetApp", hookee);
        //editor.putBoolean("isReg", isReg);
        editor.commit();
        if(sharedFile!=null) try {
            BufferedWriter writer=new BufferedWriter(new FileWriter(sharedFile));
            writer.write(hookee);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        info.setText("Target App:\r\n" + hookee);
        appname.setText(hookee);
        //regEx.setChecked(isReg);
    }

    class AppAdapter extends BaseAdapter {
        Context context;
        List<PackageInfo> packageInfo;

        AppAdapter(Context context) {
            this.context = context;
            packageInfo = context.getPackageManager().getInstalledPackages(0);
        }

        @Override
        public int getCount() {
            return packageInfo.size();
        }

        @Override
        public Object getItem(int i) {
            return packageInfo.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RelativeLayout relativeLayout = new RelativeLayout(context);
            relativeLayout.setLayoutParams(new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            ImageView iv = new ImageView(context);
            iv.setImageDrawable(packageInfo.get(i).applicationInfo.loadIcon(context.getPackageManager()));
            iv.setMaxHeight(100);
            iv.setMaxWidth(100);
            TextView tv = new TextView(context);
            tv.setPadding(80, 0, 0, 0);
            tv.setText(packageInfo.get(i).applicationInfo.loadLabel(context.getPackageManager()) + "\r\n" + packageInfo.get(i).packageName);
            relativeLayout.addView(iv);
            relativeLayout.addView(tv);
            return relativeLayout;
        }
    }
}
