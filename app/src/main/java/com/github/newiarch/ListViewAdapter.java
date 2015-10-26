package com.github.newiarch;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ListViewAdapter extends ArrayAdapter<LineItem> {
	private Context context;
	private int layoutResourceId;
	private ArrayList<LineItem> data = new ArrayList<LineItem>();

	public ListViewAdapter(Context context, int layoutResourceId,
			ArrayList<LineItem> data) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewHolder holder = null;

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
			holder = new ViewHolder();
			holder.imageTitle = (TextView) row.findViewById(R.id.text);
			holder.lastModified = (TextView) row.findViewById(R.id.lastModText);
			row.setTag(holder);
		} else {
			holder = (ViewHolder) row.getTag();
		}

		LineItem item = data.get(position);
		holder.imageTitle.setText(item.getTitle());
		holder.lastModified.setText(item.getLastModified());
		return row;
	}

	static class ViewHolder {
		TextView imageTitle;
		TextView lastModified;
	}
}