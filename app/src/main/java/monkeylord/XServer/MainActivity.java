package monkeylord.XServer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.List;

public class MainActivity extends Activity {
    SharedPreferences sp;
    String hookee;
    boolean isReg;
    TextView info;
    EditText appname;
    //CheckBox regEx;

    private static boolean isModuleActive() {
        return false;
    }

    public void makeWorldReadable(){
        new File("/data/data/" + XServer.class.getPackage().getName().toLowerCase()).setExecutable(true, false);
        new File("/data/data/" + XServer.class.getPackage().getName().toLowerCase() + "/shared_prefs/XServer.xml").setReadable(true, false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        super.setContentView(layout, param);
        sp = getSharedPreferences("XServer", isModuleActive()?MODE_WORLD_READABLE:MODE_PRIVATE);
        makeWorldReadable();
        hookee = sp.getString("targetApp", "com.");
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
                "Or" +
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
            //调整了一下应用选择器的外观，感谢@smartdone大佬建议和帮助
            //https://github.com/monkeylord/XServer/pull/1/commits/ab718e13a8ef1486f43e1023f62e312b3ff10307
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setLayoutParams(new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            //创建图标
            ImageView iv_app_icon = new ImageView(context);
            iv_app_icon.setImageDrawable(packageInfo.get(i).applicationInfo.loadIcon(context.getPackageManager()));
            iv_app_icon.setLayoutParams(new ViewGroup.LayoutParams(80, 80));
            //iv_app_icon.setAdjustViewBounds(true);
            iv_app_icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iv_app_icon.setPadding(5,5,5,5);
            linearLayout.addView(iv_app_icon);
            //创建文本描述
            LinearLayout textLayout=new LinearLayout(context);
            textLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            textLayout.setOrientation(LinearLayout.VERTICAL);

            TextView app_display_name = new TextView(context);
            app_display_name.setPadding(5, 15, 5, 3);
            app_display_name.getPaint().setFakeBoldText(true);
            app_display_name.setText(packageInfo.get(i).applicationInfo.loadLabel(context.getPackageManager()));

            TextView app_package_name = new TextView(context);
            app_package_name.setPadding(5, 3, 5, 5);
            app_package_name.setText( packageInfo.get(i).packageName);

            textLayout.addView(app_display_name);
            textLayout.addView(app_package_name);

            linearLayout.addView(textLayout);
            Log.e("XServer", "Selector Ready");
            return linearLayout;
        }
    }
}
