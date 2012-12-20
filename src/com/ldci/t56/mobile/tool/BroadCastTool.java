
package com.ldci.t56.mobile.tool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.ldci.t56.mobile.db.DbAdapter;
import com.ldci.t56.mobile.info.Call_Record_Info;
import com.ldci.t56.mobile.info.Message_Rubbish_Info;
import com.ldci.t56.mobile.info.PhoneInfo;
import com.ldci.t56.mobile.info.SmsInfo;
import com.ldci.t56.mobile.safe.R;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;

/**
 * 广播类，监听短信
 * 
 * @author emmet7life@yahoo.cn
 */
public class BroadCastTool extends BroadcastReceiver {
    static HashMap<String, PhoneInfo> mPhoneMap = new HashMap<String, PhoneInfo>();
    static HashMap<String, SmsInfo> mSMSMap = new HashMap<String, SmsInfo>();
    public static final String ENABLE_PROTECTION = "enableProtection";
    public static final String EXIT_PROTECTION_ACTION = "android.provider.Telephony.EXIT_PROTECTION_ACTION";
    public static final String UPDATE_PREFERENCE_ACTION = "android.provider.Telephony.UPDATE_PREFERENCE";
    public static final String SMS_SYSTEM_ACTION = "android.provider.Telephony.SMS_RECEIVED";// 接收短信的ACTION标识
    public static final String SMS_RECEIVED_ACTION = "com.ldci.t56.mobile.safe.SMS_RECEIVED_ACTION";// 当收到垃圾短信时发出广播的ACTION标识
    public static final String CALL_RECEIVED_ACTION = "com.ldci.t56.mobile.safe.CALL_RECEIVED_ACTION";// 当收到垃圾短信时发出广播的ACTION标识
    public static final String AUTO_START_SERVICE = "com.ldci.t56.mobile.safe.AUTO_START_SERVICE";// 接收系统启动的广播
    public static String SMS_PHONENUMBER;// 接收短信号码
    public static String SMS_CONTENT;// 接收短信内容
    private static ITelephony iTelephony;// 挂断电话的一个对象
    private static TelephonyManager telephonyMgr = null;// 电话管理类
    public static SharedPreferences mSharedPreferences = null;// 存储基本数据类的共享类
    private static boolean isAutoStartWithPhone;// 是否随系统启动
    private static int mUndisturbedMode;// 夜间模式信息
    private static final String TAG = "BroadCastTool";
    private static MyPhoneStateListener mMPSL = null;
    public static boolean enableProtection = true;

    public BroadCastTool() {
        SMS_PHONENUMBER = new String();
        SMS_CONTENT = new String();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Recieve: " + intent.getAction() + " this:" + this);
        if (null == mSharedPreferences) {
            mSharedPreferences = context
                    .getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        }

        // 更新设置
        if (intent.getAction().equals(UPDATE_PREFERENCE_ACTION)) {
            isAutoStartWithPhone = mSharedPreferences.getBoolean("isAutoStartWithPhone", false);
            mUndisturbedMode = mSharedPreferences.getInt("UndisturbedMode", 0);
            enableProtection = intent.getExtras().getBoolean(ENABLE_PROTECTION, false);
            Log.d(TAG, "enableProtection: " + enableProtection + " mUndisturbedMode:"
                    + mUndisturbedMode + " isAutoStartWithPhone:" + isAutoStartWithPhone);
            return;
        }

        if (!enableProtection) {
            return;
        }

        // 退出防护
        if (intent.getAction().equals(EXIT_PROTECTION_ACTION)) {
            enableProtection = false;
            if (telephonyMgr != null) {
                telephonyMgr.listen(mMPSL, PhoneStateListener.LISTEN_NONE);
                mMPSL = null;
            }
            Log.d(TAG, "Exit Protection");
            return;
        }

        // 监听短信广播，实现拦截垃圾短信
        if (intent.getAction().equals(SMS_SYSTEM_ACTION)) {
            // 2.1.当可以接收短信的时候，首先解析短信号码和内容，接着判断号码是否
            // 为短信黑名单中的号码，如果是则直接屏蔽，并把短信放到短信垃圾箱中
            StringBuilder mMessagePhone = new StringBuilder();
            StringBuilder mMessageContent = new StringBuilder();
            Bundle mBundle = intent.getExtras();
            if (null != mBundle) {
                Object[] mObject = (Object[]) mBundle.get("pdus");
                SmsMessage[] mMessage = new SmsMessage[mObject.length];
                for (int i = 0; i < mObject.length; i++) {
                    mMessage[i] = SmsMessage.createFromPdu((byte[]) mObject[i]);
                }
                for (SmsMessage currentMessage : mMessage) {

                    mMessagePhone.append(currentMessage.getDisplayOriginatingAddress());// 读取电话号码
                    mMessageContent.append(currentMessage.getDisplayMessageBody());// 读取短信内容
                }
                SMS_PHONENUMBER = mMessagePhone.toString();
                SMS_CONTENT = mMessageContent.toString();
                Toast.makeText(
                        context,
                        context.getString(R.string.original_phone) + SMS_PHONENUMBER + "\n"
                                + context.getString(R.string.target_phone)
                                + PhoneNumberUtils.trimSmsNumber(SMS_PHONENUMBER),
                        Toast.LENGTH_LONG).show();
                // ---------------------------------------------------------------------------------------------------------------------数据库：打开
                // 判断是否在白名单，在就放行
                DbAdapter dbAdapter = new DbAdapter(context);
                try {
                    dbAdapter.open();
                    Cursor cursorPhone = dbAdapter.getPhone(
                            PhoneNumberUtils.trimSmsNumber(SMS_PHONENUMBER),
                            5);
                    if (!cursorPhone.moveToFirst()) {
                        cursorPhone.close();
                        // 2.2判断该号码是否在短信黑名单中，如果存在则拦截该短信，并保存短信内容等信息到垃圾短信数据库中
                        cursorPhone = dbAdapter.getPhone(
                                PhoneNumberUtils.trimSmsNumber(SMS_PHONENUMBER),
                                2);
                        if (cursorPhone.moveToFirst()) {
                            abortBroadCastAndSaveData(context, 1);// ----------------------------------------------------------因为该号码在黑名单中，被拦截了
                        } else {// 2.3如果不在黑名单中，则接下来的工作就是判断短信内容了
                            mSharedPreferences = context.getSharedPreferences("SharedPreferences",
                                    Context.MODE_PRIVATE);// 读取配置文件中的敏感词信息
                            String xmlInfo = mSharedPreferences.getString("sensitive", "");
                            if (xmlInfo.length() != 0) {// 当敏感词数据不为空的时候判断
                                String[] mArray = xmlInfo.substring(0, xmlInfo.length()).split(",");// 貌似可以不用去最后一个逗号直接拆分
                                for (int i = 0; i != mArray.length; i++) {
                                    if (SMS_CONTENT.contains(mArray[i])) {
                                        abortBroadCastAndSaveData(context, 2);// ----------------------------------------------因为该短信内容含敏感词，被拦截了
                                        break;
                                    }
                                }
                            }

                        }
                    }
                    cursorPhone.close();
                } finally {
                    dbAdapter.close();
                }
            }

        }

        // 监听来电
        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            Log.d("call", "get action");
            if (null == mMPSL) { // 使用静态变量避免多次注册事件监听
                if (null == telephonyMgr) {
                    telephonyMgr = (TelephonyManager) context
                            .getSystemService(Context.TELEPHONY_SERVICE);
                }

                mMPSL = new MyPhoneStateListener(context);

                telephonyMgr.listen(mMPSL, MyPhoneStateListener.LISTEN_CALL_STATE);
                // 利用反射获取隐藏的endcall方法
                try {
                    Method getITelephonyMethod = TelephonyManager.class.getDeclaredMethod(
                            "getITelephony", (Class[]) null);
                    getITelephonyMethod.setAccessible(true);
                    iTelephony = (ITelephony) getITelephonyMethod.invoke(telephonyMgr,
                            (Object[]) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 铃声
    protected void ring(AudioManager audio) {
        audio.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
                AudioManager.VIBRATE_SETTING_OFF);
    }

    // 静音
    protected void silent(AudioManager audio) {
        audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
                AudioManager.VIBRATE_SETTING_OFF);
    }

    // 震动
    protected void vibrate(AudioManager audio) {
        audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
        audio.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
                AudioManager.VIBRATE_SETTING_ON);
    }

    // 电话状态监听类
    class MyPhoneStateListener extends PhoneStateListener {

        int i = 0;
        Context mContext;
        AudioManager audioManager;
        TelephonyManager mTM;
        int oriMode = 2;

        public MyPhoneStateListener(Context context) {
            mContext = context;
            mTM = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        /** 设置铃声为静音并挂断电话 */
        private void audioSilentEndCall() {
            oriMode = audioManager.getRingerMode();
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);// 设置为静音模式
            boolean isHungUp = false;

            int i = 0;
            try {
                isHungUp = iTelephony.endCall();// 挂断电话
                Log.d(TAG, isHungUp ? "Hung up success" : " Hung up fail, retry time is "
                        + i);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }

            // 再恢复正常铃声
            audioManager.setRingerMode(oriMode);
        }

        private void storageCallRecord(String incomingNumber) {
            DbAdapter dbAdapter = new DbAdapter(mContext);
            try {
                dbAdapter.open();
                Cursor cursor = dbAdapter
                        .getTime(DbAdapter.CALL_RECORD_TABLE_NAME,
                                DbAdapter.CALL_RECORD_TIME,
                                GetCurrentTime.getFormateDate());
                if (!cursor.moveToFirst()) {
                    Call_Record_Info mCRI = new Call_Record_Info();
                    mCRI.setCall_record_time(GetCurrentTime.getFormateDate());
                    mCRI.setCall_record_phone(PhoneNumberUtils
                            .trimSmsNumber(incomingNumber));
                    dbAdapter.getAdd(mCRI);
                    Intent mIntent = new Intent();
                    mIntent.setAction(CALL_RECEIVED_ACTION);
                    mContext.sendBroadcast(mIntent);
                }
                cursor.close();
            } finally {
                dbAdapter.close();
            }
        }

        private void deleteCallLog(String incomingNumber) {
            Cursor cursorRecord = mContext.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI, null, null, null,
                    CallLog.Calls.DEFAULT_SORT_ORDER);
            int mCount1 = cursorRecord.getCount();
            cursorRecord.close();
            boolean mDeleteCallLog = true;
            while (mDeleteCallLog) {
                Cursor cursorDeleteAccount = mContext.getContentResolver()
                        .query(
                                CallLog.Calls.CONTENT_URI, null, null, null,
                                CallLog.Calls.DEFAULT_SORT_ORDER);
                int mCount2 = cursorDeleteAccount.getCount();
                cursorDeleteAccount.close();
                if (mCount2 != mCount1) {
                    mDeleteCallLog = false;
                    int mId = 0;
                    Cursor callCursor = mContext.getContentResolver().query(
                            CallLog.Calls.CONTENT_URI, null, "number" + "=?",
                            new String[] {
                                incomingNumber
                            }, CallLog.Calls.DEFAULT_SORT_ORDER);
                    if (callCursor.moveToNext()) {
                        mId = callCursor.getInt(callCursor.getColumnIndex("_id"));
                    }
                    mId = callCursor.getInt(callCursor.getColumnIndex("_id"));
                    mContext.getContentResolver().delete(
                            CallLog.Calls.CONTENT_URI, "_id" + "=?",
                            new String[] {
                                String.valueOf(mId)
                            });
                    callCursor.close();
                }
            }
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:// 待机状态
                    Log.d(TAG, "CALL_STATE_IDLE");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.d(TAG, "CALL_STATE_OFFHOOK");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:// 来电状态
                    Log.d(TAG, "CALL_STATE_RINGING");
                    // ServiceTool.serviceInt++;
                    // 当是来电状态的时候需要判断“设置中的”拦截电话“的配置信息，如果是勾选的，则直接拒接掉所有电话
                    boolean needStorage = false;
                    // 判断是否在白名单，在就放行
                    DbAdapter dbAdapter = new DbAdapter(mContext);
                    try {
                        dbAdapter.open();
                        Cursor phoneCursor = dbAdapter.getPhone(
                                PhoneNumberUtils.trimSmsNumber(incomingNumber),
                                5);
                        if (!phoneCursor.moveToFirst()) {
                            phoneCursor.close();
                            if ((2 == mUndisturbedMode || 3 == mUndisturbedMode)
                                    && isIncludedTime(mContext)) {
                                audioSilentEndCall();
                                needStorage = true;
                            } else {
                                // 判断该号码是否在黑名单中，如果是则挂断，并存储来电信息到数据库中
                                phoneCursor = dbAdapter.getPhone(
                                        PhoneNumberUtils.trimSmsNumber(incomingNumber), 4);
                                if (phoneCursor.moveToFirst()) {
                                    audioSilentEndCall();
                                    needStorage = true;
                                }
                            }
                        }
                        phoneCursor.close();
                        if (needStorage) {
                            // 保存数据
                            storageCallRecord(incomingNumber);
                            // 删除手机电话记录
                            deleteCallLog(incomingNumber);

                        }
                    } finally {
                        dbAdapter.close();
                    }
                    break;

            }
        }
    }

    /** 判断当前时间是否在夜间免扰模式的时间段内 */
    private boolean isIncludedTime(Context context) {
        long startTime = mSharedPreferences.getLong("UndisturbedStartTime", -1);
        long endTime = mSharedPreferences.getLong("UndisturbedEndTime", -1);
        if (-1 == startTime || -1 == endTime)
            return false;
        Calendar calendar = Calendar.getInstance();
        long currentTime = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        if (currentTime >= startTime && currentTime <= endTime) {
            return true;
        }
        if (endTime >= 1440 && currentTime <= endTime - 1440) {
            return true;
        }
        return false;
    }

    /** 中止广播并存放数据到垃圾箱数据库中 */
    private void abortBroadCastAndSaveData(Context context, int i) {
        BroadCastTool.this.abortBroadcast();// 中止短信广播：当收到垃圾短信之后，存放垃圾信息到数据库中，然后中止广播
        // 数据库操作：插入该垃圾短信数据到数据库中
        Message_Rubbish_Info mRMI = new Message_Rubbish_Info();
        mRMI.setMessage_rubbish_phone(SMS_PHONENUMBER);// ---------------------------短信号码
        mRMI.setMessage_rubbish_content(SMS_CONTENT);// -----------------------------------短信内容
        mRMI.setMessage_rubbish_time(GetCurrentTime.getFormateDate());// -----------------收件时间
        DbAdapter dbAdapter = new DbAdapter(context);
        try {
            dbAdapter.open();
            dbAdapter.getAdd(mRMI);
        } finally {
            dbAdapter.close();
        }
        // 拦截到垃圾短信或者黑名单短信之后发送广播，刷新短信息的拦截记录页面
        Intent mIntent = new Intent();
        mIntent.setAction(SMS_RECEIVED_ACTION);
        context.sendBroadcast(mIntent);

        switch (i) {
            case 1:
                Toast.makeText(
                        context,
                        String.format(context.getString(R.string.black_number_message),
                                SMS_PHONENUMBER, SMS_CONTENT, GetCurrentTime.getFormateDate()),
                        Toast.LENGTH_LONG).show();
                break;
            case 2:
                Toast.makeText(
                        context,
                        String.format(context.getString(R.string.black_work_message),
                                SMS_PHONENUMBER, SMS_CONTENT, GetCurrentTime.getFormateDate()),
                        Toast.LENGTH_LONG).show();
                break;
        }

    }
}
