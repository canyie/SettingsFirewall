package top.canyie.settingsfirewall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * @author canyie
 */
public class SettingListAdapter extends ArrayAdapter<Replacement> {
    private final SettingsEditActivity activity;
    public SettingListAdapter(SettingsEditActivity activity, List<Replacement> list) {
        super(activity, 0, list);
        this.activity = activity;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        var replacement = getItem(position);
        assert replacement != null;
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.setting_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.key = convertView.findViewById(R.id.key);
            viewHolder.replacement = convertView.findViewById(R.id.replacement);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        convertView.setOnClickListener(v -> activity.onItemClicked(replacement));
        viewHolder.key.setText(replacement.key);
        if (replacement.value != null) {
            viewHolder.replacement.setVisibility(View.VISIBLE);
            viewHolder.replacement.setText(getContext().getString(R.string.replaced_with, replacement.value));
        } else {
            viewHolder.replacement.setVisibility(View.GONE);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView key;
        TextView replacement;
    }
}
