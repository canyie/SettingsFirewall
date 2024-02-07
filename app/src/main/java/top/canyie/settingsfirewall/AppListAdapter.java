package top.canyie.settingsfirewall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * @author canyie
 */
public class AppListAdapter extends ArrayAdapter<AppInfo> {
    private final MainActivity activity;
    public AppListAdapter(MainActivity activity, List<AppInfo> list) {
        super(activity, 0, list);
        this.activity = activity;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        var appInfo = getItem(position);
        assert appInfo != null;
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).inflate(R.layout.app_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.name = convertView.findViewById(R.id.app_name);
            viewHolder.icon = convertView.findViewById(R.id.app_icon);
            viewHolder.checkBox = convertView.findViewById(R.id.checkbox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        convertView.setOnClickListener(v -> activity.onItemClicked(appInfo));
        viewHolder.name.setText(appInfo.name);
        viewHolder.icon.setImageDrawable(appInfo.icon);
        viewHolder.checkBox.setOnCheckedChangeListener(null);
        viewHolder.checkBox.setChecked(appInfo.enabled);
        viewHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                activity.onItemChecked(appInfo, isChecked));
        return convertView;
    }

    private static class ViewHolder {
        TextView name;
        ImageView icon;
        CheckBox checkBox;
    }
}
