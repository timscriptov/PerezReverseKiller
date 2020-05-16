package com.perez.catchexception;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;
import android.support.multidex.MultiDex;

import com.github.karthyks.crashlytics.Crashlytics;
import com.github.karthyks.crashlytics.EventListener;
import com.github.karthyks.crashlytics.data.Event;
import java.util.List;
import android.util.Log;

public class CrashApp extends Application {
    CrashApp instance;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
        instance = this;
    }

    @Override
    public String getPackageName() {
        if(Log.getStackTraceString(new Throwable()).contains("com.xunlei.downloadlib")) {
            return "com.xunlei.downloadprovider";
        }
        return super.getPackageName();
    }
    @Override
    public PackageManager getPackageManager() {
        if(Log.getStackTraceString(new Throwable()).contains("com.xunlei.downloadlib")) {
            return new DelegateApplicationPackageManager(super.getPackageManager());
        }
        return super.getPackageManager();
    }

    public void onCreate() {
        super.onCreate();
        Crashlytics.init(this, new EventListener() {
            @Override
            public void onEventOccurred(List<Event> events) throws Exception {
                // Log to your Cloud DB for future analytics.
                Log.d("MainApplication", "onEventOccurred: " + events.size());
            }
        });
        try {
            System.loadLibrary("function");
        } catch(UnsatisfiedLinkError u) {
            u.printStackTrace();
            Toast.makeText(getApplicationContext(), "Perez管理器功能库无法加载,程序即将退出!", Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(3000);
            } catch(InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }
}
