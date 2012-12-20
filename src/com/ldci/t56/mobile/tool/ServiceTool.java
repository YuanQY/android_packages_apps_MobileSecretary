
package com.ldci.t56.mobile.tool;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.ldci.t56.mobile.safe.MainActivity;
import com.ldci.t56.mobile.safe.R;

public class ServiceTool extends Service {
    
    private static final String TAG = "ServiceTool";  
    private NotificationManager mNF;
    public static String AUTO_START = "com.ldci.android.t56.mobile.safe.AUTO_START";

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // public static int serviceInt = 0;
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        // Toast.makeText(this, "开启服务，发出通知", Toast.LENGTH_LONG).show();
        // new Thread(new Runnable(){
        // public void run(){
        // while(true){
        // try {
        // Thread.sleep(1000);
        // serviceInt = 0;
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // }
        // }
        // }).start();
        Intent intentStart = new Intent(BroadCastTool.UPDATE_PREFERENCE_ACTION);
        intentStart.putExtra(BroadCastTool.ENABLE_PROTECTION, true);
        sendBroadcast(intentStart); 
        autoStartNotification();
        Log.d(TAG, "Start Mobile Secretary service.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Toast.makeText(this, "停止服务，取消通知", Toast.LENGTH_LONG).show();
        mNF.cancel(R.string.app_name);
        Log.d(TAG, "Stop Mobile Secretary service.");
    }

    private void autoStartNotification() {
        Context context = this.getApplicationContext();
        Notification mNotification = new Notification(R.drawable.app_logo,
                context.getText(R.string.app_name),
                System.currentTimeMillis());
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(AUTO_START);
        intent.putExtra("auto_start", "boot_completed");
        PendingIntent mPI = PendingIntent.getActivity(this, 0, intent, 0);
        mNotification.setLatestEventInfo(this, context.getText(R.string.app_name),
                context.getText(R.string.start_on_boot_success), mPI);
        if (null == mNF) {
            mNF = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        mNF.notify(R.string.app_name, mNotification);
    }

}
