<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.customkeyboard">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.READ_CLIPBOARD" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="স্মার্ট কীবোর্ড প্রো"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar"
        android:supportsRtl="true">
        
        <!-- কীবোর্ড সার্ভিস -->
        <service
            android:name=".KeyboardIME"
            android:permission="android.permission.BIND_INPUT_METHOD"
            android:exported="true">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>
        
        <!-- সেটিংস অ্যাক্টিভিটি -->
        <activity
            android:name=".SettingsActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>
</manifest>
