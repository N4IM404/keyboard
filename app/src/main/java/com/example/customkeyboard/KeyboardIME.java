package com.example.customkeyboard;

import android.content.*;
import android.inputmethodservice.InputMethodService;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import java.util.*;

public class KeyboardIME extends InputMethodService {
    
    private LinearLayout mainKeyboardView;
    private LinearLayout topFeatureBar;
    private LinearLayout keypadContainer;
    private InputConnection inputConnection;
    
    private boolean isBangla = true;
    private boolean shiftOn = false;
    
    private ClipboardManager clipboardManager;
    private Vibrator vibrator;
    private Handler holdHandler;
    private CopyBookManager copyBookManager;
    
    // হোল্ড জেসচার
    private static final long HOLD_DURATION = 500;
    private boolean isHolding = false;
    private String currentHoldKey = "";
    
    // কম্পিউটার স্টাইল বাংলা কীবোর্ড লেআউট (বিজয়/অভ্র স্টাইল)
    private String[][] banglaComputerLayout = {
        // ১ম সারি
        {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "⌫"},
        // ২য় সারি  
        {"Tab", "্", "।", "ঃ", "ঙ", "ঞ", "চ", "ছ", "জ", "ঝ", "ণ", "ঢ", "ণ"},
        // ৩য় সারি
        {"Caps", "ব", "য", "ড", "ঢ", "ণ", "ত", "দ", "প", "ঠ", "ক", "এন্টার"},
        // ৪র্থ সারি
        {"⇧", "র", "স", "ত", "দ", "ধ", "ন", "ল", ";", "'", "⇧"},
        // ৫ম সারি
        {"Ctrl", "Win", "Alt", "স্পেস", "AltGr", "Ctrl"}
    };
    
    private String[][] englishLayout = {
        {"`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "⌫"},
        {"Tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]"},
        {"Caps", "a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'", "Enter"},
        {"⇧", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "⇧"},
        {"Ctrl", "Win", "Alt", "Space", "AltGr", "Ctrl"}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        holdHandler = new Handler();
        copyBookManager = new CopyBookManager(this);
    }

    @Override
    public View onCreateInputView() {
        // মেইন লেআউট তৈরি
        mainKeyboardView = new LinearLayout(this);
        mainKeyboardView.setOrientation(LinearLayout.VERTICAL);
        mainKeyboardView.setBackgroundColor(0xFF2D2D2D);
        
        // টপ ফিচার বার
        createTopFeatureBar();
        
        // কিপ্যাড কন্টেইনার
        keypadContainer = new LinearLayout(this);
        keypadContainer.setOrientation(LinearLayout.VERTICAL);
        keypadContainer.setPadding(4, 4, 4, 8);
        
        buildKeyboardLayout();
        
        mainKeyboardView.addView(topFeatureBar);
        mainKeyboardView.addView(keypadContainer);
        
        return mainKeyboardView;
    }

    private void createTopFeatureBar() {
        topFeatureBar = new LinearLayout(this);
        topFeatureBar.setOrientation(LinearLayout.HORIZONTAL);
        topFeatureBar.setBackgroundColor(0xFF1A1A1A);
        topFeatureBar.setPadding(8, 6, 8, 6);
        
        // ইমোজি বাটন
        Button emojiBtn = createFeatureButton("😊");
        emojiBtn.setOnClickListener(v -> showEmojiPicker());
        
        // কপি বুক বাটন
        Button copyBookBtn = createFeatureButton("📋 কপি বুক");
        copyBookBtn.setOnClickListener(v -> showCopyBook());
        
        // ট্রান্সলেট বাটন
        Button translateBtn = createFeatureButton("🌐 অনুবাদ");
        translateBtn.setOnClickListener(v -> openGoogleTranslate());
        
        // ল্যাঙ্গুয়েজ টগল
        Button langToggleBtn = createFeatureButton(isBangla ? "🇧🇩 বাংলা" : "🇬🇧 English");
        langToggleBtn.setOnClickListener(v -> {
            isBangla = !isBangla;
            langToggleBtn.setText(isBangla ? "🇧🇩 বাংলা" : "🇬🇧 English");
            keypadContainer.removeAllViews();
            buildKeyboardLayout();
        });
        
        // স্পেসার
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1.0f));
        
        topFeatureBar.addView(emojiBtn);
        topFeatureBar.addView(copyBookBtn);
        topFeatureBar.addView(translateBtn);
        topFeatureBar.addView(spacer);
        topFeatureBar.addView(langToggleBtn);
    }

    private Button createFeatureButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundColor(0xFF3D3D3D);
        btn.setPadding(12, 8, 12, 8);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        btn.setLayoutParams(params);
        
        return btn;
    }

    private void buildKeyboardLayout() {
        String[][] currentLayout = isBangla ? banglaComputerLayout : englishLayout;
        
        for (int rowIndex = 0; rowIndex < currentLayout.length; rowIndex++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 2, 0, 2);
            rowLayout.setLayoutParams(rowParams);
            
            for (String key : currentLayout[rowIndex]) {
                Button keyBtn = createKeyButton(key, rowIndex);
                rowLayout.addView(keyBtn);
            }
            
            keypadContainer.addView(rowLayout);
        }
    }

    private Button createKeyButton(final String key, int rowIndex) {
        Button btn = new Button(this);
        btn.setText(key);
        btn.setTextSize(14);
        btn.setTextColor(0xFFFFFFFF);
        
        // স্পেশাল কীগুলোর জন্য আলাদা সাইজ
        LinearLayout.LayoutParams params;
        float weight = 1.0f;
        
        if (key.equals("স্পেস") || key.equals("Space")) {
            weight = 4.0f;
            btn.setText("");
            btn.setBackgroundColor(0xFF4A4A4A);
        } else if (key.equals("⌫") || key.equals("Tab") || key.equals("Caps")) {
            weight = 1.5f;
            btn.setBackgroundColor(0xFF4A4A4A);
        } else if (key.equals("এন্টার") || key.equals("Enter")) {
            weight = 2.0f;
            btn.setBackgroundColor(0xFF00BCD4);
        } else if (key.equals("Ctrl") || key.equals("Win") || key.equals("Alt") || 
                   key.equals("AltGr")) {
            weight = 1.2f;
            btn.setBackgroundColor(0xFF4A4A4A);
        } else if (key.equals("⇧")) {
            weight = 1.8f;
            btn.setBackgroundColor(shiftOn ? 0xFFFF9800 : 0xFF4A4A4A);
        } else {
            btn.setBackgroundColor(0xFF616161);
        }
        
        params = new LinearLayout.LayoutParams(
            0,
            80,
            weight
        );
        params.setMargins(2, 1, 2, 1);
        btn.setLayoutParams(params);
        
        // হোল্ড জেসচার
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        currentHoldKey = key;
                        isHolding = false;
                        holdHandler.postDelayed(holdRunnable, HOLD_DURATION);
                        vibrate(20);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        holdHandler.removeCallbacks(holdRunnable);
                        if (!isHolding) {
                            handleKeyPress(key);
                        }
                        isHolding = false;
                        return true;
                }
                return false;
            }
        });
        
        return btn;
    }

    private Runnable holdRunnable = new Runnable() {
        @Override
        public void run() {
            isHolding = true;
            vibrate(50);
            handleHoldAction(currentHoldKey);
        }
    };

    private void handleKeyPress(String key) {
        inputConnection = getCurrentInputConnection();
        if (inputConnection == null) return;
        
        switch (key) {
            case "⌫":
                inputConnection.deleteSurroundingText(1, 0);
                break;
                
            case "এন্টার":
            case "Enter":
                inputConnection.commitText("\n", 1);
                break;
                
            case "স্পেস":
            case "Space":
                inputConnection.commitText(" ", 1);
                break;
                
            case "Tab":
                inputConnection.commitText("\t", 1);
                break;
                
            case "⇧":
                shiftOn = !shiftOn;
                Toast.makeText(this, shiftOn ? "Shift On" : "Shift Off", Toast.LENGTH_SHORT).show();
                break;
                
            default:
                if (key.length() == 1) {
                    String text = shiftOn ? key.toUpperCase() : key;
                    inputConnection.commitText(text, 1);
                }
                break;
        }
    }

    private void handleHoldAction(String key) {
        inputConnection = getCurrentInputConnection();
        
        if (key.equalsIgnoreCase("c")) {
            // কপি
            performCopy();
        } else if (key.equalsIgnoreCase("v")) {
            // পেস্ট
            performPaste();
        } else if (key.equalsIgnoreCase("x")) {
            // কাট
            performCut();
        }
    }

    private void performCopy() {
        if (inputConnection != null) {
            CharSequence selected = inputConnection.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                copyBookManager.addCopyItem(selected.toString());
                showToast("কপি বুক এ যোগ হয়েছে ✓");
            }
        }
    }

    private void performPaste() {
        copyBookManager.showCopyBook(this, new CopyBookManager.OnItemSelectedListener() {
            @Override
            public void onItemSelected(String text) {
                if (inputConnection != null) {
                    inputConnection.commitText(text, 1);
                }
            }
        });
    }

    private void performCut() {
        if (inputConnection != null) {
            CharSequence selected = inputConnection.getSelectedText(0);
            if (selected != null && selected.length() > 0) {
                copyBookManager.addCopyItem(selected.toString());
                inputConnection.commitText("", 1);
                showToast("কাট করে কপি বুক এ যোগ ✓");
            }
        }
    }

    private void showCopyBook() {
        copyBookManager.showCopyBook(this, new CopyBookManager.OnItemSelectedListener() {
            @Override
            public void onItemSelected(String text) {
                if (inputConnection != null) {
                    inputConnection.commitText(text, 1);
                }
            }
        });
    }

    private void showEmojiPicker() {
        String[] emojis = {
            "😀", "😂", "🤣", "😊", "😇", "😍", "🤩", "😎", "😢", "😭",
            "😡", "😱", "🥺", "❤️", "🔥", "⭐", "🎉", "👍", "👎", "✌️",
            "🤝", "🙏", "💪", "🏠", "💰", "📱", "🚗", "🌈", "🍕", "🎮",
            "😴", "🤔", "😷", "🤒", "💀", "👻", "🎃", "🎄", "🎁", "🏆"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ইমোজি বাছুন");
        
        GridView gridView = new GridView(this);
        gridView.setNumColumns(5);
        gridView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, emojis));
        
        builder.setView(gridView);
        AlertDialog dialog = builder.create();
        
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (inputConnection != null) {
                inputConnection.commitText(emojis[position], 1);
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void openGoogleTranslate() {
        String selectedText = "";
        if (inputConnection != null) {
            CharSequence text = inputConnection.getSelectedText(0);
            if (text != null) selectedText = text.toString();
        }
        
        String url = "https://translate.google.com/?text=" + selectedText;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void vibrate(long duration) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(duration);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
          }
