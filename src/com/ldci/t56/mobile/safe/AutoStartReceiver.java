
package com.ldci.t56.mobile.safe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.ldci.t56.mobile.tool.BroadCastTool;
import com.ldci.t56.mobile.tool.ServiceTool;

public class AutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive: BOOT_COMPLETED");
        // TODO Auto-generated method stub
        SharedPreferences mSharedPreferences = context
                .getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        boolean isAutoStartWithPhone = mSharedPreferences.getBoolean("isAutoStartWithPhone", false);
        Log.v(TAG, "isAutoStartWithPhone: " + isAutoStartWithPhone);

        if (isAutoStartWithPhone) {
            Intent mIntent = new Intent(context, ServiceTool.class);
            context.startService(mIntent);// 启动服务
            BroadCastTool.enableProtection = true;
        }
    }

}
