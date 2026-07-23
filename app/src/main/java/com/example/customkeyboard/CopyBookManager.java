package com.example.customkeyboard;

import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import android.app.AlertDialog;

public class CopyBookManager {
    
    private static final String PREFS_NAME = "CopyBookPrefs";
    private static final String KEY_ITEMS = "copy_items";
    private static final int MAX_ITEMS = 10;
    
    private SharedPreferences prefs;
    private List<CopyBookItem> items;
    
    public interface OnItemSelectedListener {
        void onItemSelected(String text);
    }
    
    public CopyBookManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadItems();
    }
    
    private void loadItems() {
        items = new ArrayList<>();
        String savedJson = prefs.getString(KEY_ITEMS, "");
        if (!savedJson.isEmpty()) {
            String[] parts = savedJson.split("\\|\\|\\|");
            for (String part : parts) {
                String[] itemParts = part.split("\\|\\|");
                if (itemParts.length >= 2) {
                    CopyBookItem item = new CopyBookItem(
                        itemParts[0],
                        Boolean.parseBoolean(itemParts[1]),
                        Long.parseLong(itemParts[2])
                    );
                    items.add(item);
                }
            }
        }
    }
    
    private void saveItems() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            CopyBookItem item = items.get(i);
            sb.append(item.text)
              .append("||")
              .append(item.isPinned)
              .append("||")
              .append(item.timestamp);
            if (i < items.size() - 1) {
                sb.append("|||");
            }
        }
        prefs.edit().putString(KEY_ITEMS, sb.toString()).apply();
    }
    
    public void addCopyItem(String text) {
        // ডুপ্লিকেট চেক
        for (CopyBookItem item : items) {
            if (item.text.equals(text)) {
                // ইতিমধ্যে আছে, টাইমস্ট্যাম্প আপডেট
                item.timestamp = System.currentTimeMillis();
                saveItems();
                return;
            }
        }
        
        CopyBookItem newItem = new CopyBookItem(text, false, System.currentTimeMillis());
        items.add(0, newItem); // নতুন আইটেম উপরে যোগ
        
        // ১০ এর বেশি হলে পুরাতন রিমুভ (পিন করা বাদে)
        removeOldItemsIfNeeded();
        
        saveItems();
    }
    
    private void removeOldItemsIfNeeded() {
        // পিন করা আইটেম আলাদা
        List<CopyBookItem> pinnedItems = new ArrayList<>();
        List<CopyBookItem> unpinnedItems = new ArrayList<>();
        
        for (CopyBookItem item : items) {
            if (item.isPinned) {
                pinnedItems.add(item);
            } else {
                unpinnedItems.add(item);
            }
        }
        
        // আনপিন করা আইটেম ১০ এর বেশি হলে পুরাতন রিমুভ
        while (unpinnedItems.size() > MAX_ITEMS) {
            // সবচেয়ে পুরাতন খুঁজে রিমুভ
            CopyBookItem oldest = unpinnedItems.get(0);
            for (CopyBookItem item : unpinnedItems) {
                if (item.timestamp < oldest.timestamp) {
                    oldest = item;
                }
            }
            unpinnedItems.remove(oldest);
        }
        
        // পুনরায় লিস্ট তৈরি
        items.clear();
        items.addAll(pinnedItems);
        items.addAll(unpinnedItems);
    }
    
    public void togglePin(int position) {
        if (position < items.size()) {
            CopyBookItem item = items.get(position);
            item.isPinned = !item.isPinned;
            
            // পিন আনপিন করলে টাইমস্ট্যাম্প আপডেট
            item.timestamp = System.currentTimeMillis();
            
            // পিন করা আইটেম উপরে আনুন
            if (item.isPinned) {
                items.remove(position);
                items.add(0, item);
            }
            
            saveItems();
        }
    }
    
    public void removeItem(int position) {
        if (position < items.size()) {
            items.remove(position);
            saveItems();
        }
    }
    
    public void showCopyBook(Context context, OnItemSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("📋 কপি বুক (সর্বোচ্চ ১০)");
        
        View view = LayoutInflater.from(context).inflate(
            android.R.layout.list_content, null);
        
        ListView listView = new ListView(context);
        CopyBookAdapter adapter = new CopyBookAdapter(context, items, new CopyBookAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(String text) {
                listener.onItemSelected(text);
            }
            
            @Override
            public void onPinClick(int position) {
                togglePin(position);
                adapter.notifyDataSetChanged();
            }
            
            @Override
            public void onDeleteClick(int position) {
                removeItem(position);
                adapter.notifyDataSetChanged();
                if (items.isEmpty()) {
                    // ডায়ালগ বন্ধ
                }
            }
        });
        
        listView.setAdapter(adapter);
        
        builder.setView(listView);
        builder.setNegativeButton("বন্ধ", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
