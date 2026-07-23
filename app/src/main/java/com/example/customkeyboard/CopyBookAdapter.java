package com.example.customkeyboard;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.*;

public class CopyBookAdapter extends BaseAdapter {
    
    private Context context;
    private List<CopyBookItem> items;
    private OnItemActionListener listener;
    
    public interface OnItemActionListener {
        void onItemClick(String text);
        void onPinClick(int position);
        void onDeleteClick(int position);
    }
    
    public CopyBookAdapter(Context context, List<CopyBookItem> items, OnItemActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }
    
    @Override
    public int getCount() {
        return items.size();
    }
    
    @Override
    public Object getItem(int position) {
        return items.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                android.R.layout.simple_list_item_1, parent, false);
            
            holder = new ViewHolder();
            holder.textView = convertView.findViewById(android.R.id.text1);
            
            // বাটন যোগ
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            
            holder.pinBtn = new Button(context);
            holder.pinBtn.setText("📌");
            holder.pinBtn.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
            
            holder.deleteBtn = new Button(context);
            holder.deleteBtn.setText("🗑");
            holder.deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(80, 80));
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        CopyBookItem item = items.get(position);
        holder.textView.setText((item.isPinned ? "📌 " : "") + item.text);
        
        holder.pinBtn.setOnClickListener(v -> listener.onPinClick(position));
        holder.deleteBtn.setOnClickListener(v -> listener.onDeleteClick(position));
        
        convertView.setOnClickListener(v -> listener.onItemClick(item.text));
        
        return convertView;
    }
    
    static class ViewHolder {
        TextView textView;
        Button pinBtn;
        Button deleteBtn;
    }
}
