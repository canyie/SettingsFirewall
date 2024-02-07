package top.canyie.settingsfirewall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ListView;

/**
 * @author canyie
 */
public class SettingsEditActivity extends Activity {
    public static final String KEY_NAME = "name";
    public static final String KEY_UID = "uid";
    private int uid;
    private ISettingsFirewall service;
    private SettingListAdapter adapter;
    private LayoutInflater layoutInflater;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || (uid = intent.getIntExtra(KEY_UID, -1)) == -1) {
            finish();
            return;
        }
        setContentView(R.layout.settings);
        layoutInflater = LayoutInflater.from(this);
        var actionBar = getActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setTitle(R.string.edit_settings_replacement);
        actionBar.setSubtitle(intent.getStringExtra(KEY_NAME));
        service = App.getService(this);
        adapter = new SettingListAdapter(this, App.getSettings(service, uid));
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(adapter);
    }

    public void onItemClicked(Replacement replacement) {
        var layout = layoutInflater.inflate(R.layout.edit_dialog, null);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText editText = layout.findViewById(R.id.edit);
        if (replacement.value != null)
            editText.setText(replacement.value);
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.editing, replacement.key))
                .setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    replacement.value = editText.getText().toString();
                    try {
                        service.setReplacement(uid, replacement.key, replacement.value, replacement.flags);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .setNeutralButton(R.string.delete, (dialog, which) -> {
                    replacement.value = null;
                    try {
                        service.deleteReplacement(uid, replacement.key);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                    adapter.notifyDataSetChanged();
                })
                .setCancelable(false)
                .show();
    }
}
