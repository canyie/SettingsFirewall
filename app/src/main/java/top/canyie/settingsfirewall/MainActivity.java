package top.canyie.settingsfirewall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.ListView;

/**
 * @author canyie
 */
public class MainActivity extends Activity {
    private ISettingsFirewall service;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.main);
            findViewById(android.R.id.home).setVisibility(View.GONE);
            service = App.getService(this);
            findViewById(R.id.progress_bar).setVisibility(View.GONE);
            if (service == null) {
                findViewById(R.id.not_activated_msg).setVisibility(View.VISIBLE);
                return;
            }
            ListView listView = findViewById(R.id.list);
            AppListAdapter adapter = new AppListAdapter(this, App.getSortedList(this, service));
            listView.setAdapter(adapter);
            listView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onItemChecked(AppInfo appInfo, boolean checked) {
        try {
            appInfo.enabled = checked;
            service.setTarget(appInfo.uid, checked);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void onItemClicked(AppInfo appInfo) {
        startActivity(new Intent(this, SettingsEditActivity.class)
                .putExtra(SettingsEditActivity.KEY_UID, appInfo.uid)
                .putExtra(SettingsEditActivity.KEY_NAME, appInfo.name));
    }
}
