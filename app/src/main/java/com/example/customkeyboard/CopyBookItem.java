package com.example.customkeyboard;

public class CopyBookItem {
    public String text;
    public boolean isPinned;
    public long timestamp;
    
    public CopyBookItem(String text, boolean isPinned, long timestamp) {
        this.text = text;
        this.isPinned = isPinned;
        this.timestamp = timestamp;
    }
}
