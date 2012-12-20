
package com.ldci.t56.mobile.safe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.ldci.t56.mobile.tool.BroadCastTool;
import com.ldci.t56.mobile.widget.MyImageButton;

public class MainActivity extends Activity implements OnClickListener {
    /** 成员变量 */
    private static final String TAG = "MainActivity";
    public static boolean[] isCheckBoxChecked = new boolean[4];
    public static boolean[] isRadioButtonChecked = new boolean[4];

    private MyImageButton mMenu_Message, mMenu_Call, mMenu_Set, mMenu_Sensitive, mMenu_Exit, mMenu_Whitelist;
    private Intent mIntent;
    private Intent mServiceIntent;
    public static final String MESSAGE_RUBBISH_TABLE_NAME = "message_rubbish_table";
    public static int mUndisturbedMode;
    EditText mInputPhoneNumber;
    String result;
    private boolean exitService = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUndisturbedMode = 3;// close
        Log.d("MainActivity", "onCreate");
        mServiceIntent = new Intent(BroadCastTool.AUTO_START_SERVICE);
        initContentView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");
    }

    @Override
    protected void onResume() {
        Log.d("MainActivity", "onResume");
        super.onResume();
        exitService = false;
    }

    // 监视键盘的返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(MainActivity.this).setTitle(R.string.dialog_confirm_title)
                    .setMessage(
                            R.string.dialog_confirm_exit_message)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface arg0, int arg1) {
                                    Intent intent = new Intent(
                                            BroadCastTool.AUTO_START_SERVICE);
                                    stopService(intent);
                                    MainActivity.this.finish();
                                }

                            }).setNegativeButton(R.string.cancel, null).show();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onStop() {
        Log.d("MainActivity", "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy");
        if (!exitService) {
            startService(mServiceIntent);
        } else {
            Intent intent = new Intent(BroadCastTool.EXIT_PROTECTION_ACTION);
            sendBroadcast(intent);
        }
    }

    /** 当密码正确，或者未设置密码保护时的初始化界面 */
    private void initContentView() {
        startService(mServiceIntent);
        setContentView(R.layout.main);
        initView();
        initListener();
        stopService(mServiceIntent);
    }

    /** 初始化基本组件 */
    private void initView() {
        mMenu_Message = new MyImageButton(this, R.drawable.button_message, R.string.button_message);
        LinearLayout layout = (LinearLayout) findViewById(R.id.button_message);
        layout.addView(mMenu_Message);

        mMenu_Call = new MyImageButton(this, R.drawable.button_call, R.string.button_call); //
        layout = (LinearLayout) findViewById(R.id.button_call);
        layout.addView(mMenu_Call);

        mMenu_Set = new MyImageButton(this, R.drawable.button_settings, R.string.button_settings);
        layout = (LinearLayout) findViewById(R.id.button_settings);
        layout.addView(mMenu_Set);
        
        mMenu_Whitelist = new MyImageButton(this, R.drawable.button_whitelist, R.string.button_whitelist);
        layout = (LinearLayout) findViewById(R.id.button_whitelist);
        layout.addView(mMenu_Whitelist);

        mMenu_Sensitive = new MyImageButton(this, R.drawable.button_sensitive,
                R.string.button_sensitive);
        layout = (LinearLayout) findViewById(R.id.button_sensitive);
        layout.addView(mMenu_Sensitive);

        mMenu_Exit = new MyImageButton(this, R.drawable.button_exit, R.string.button_exit);
        layout = (LinearLayout) findViewById(R.id.button_exit);
        layout.addView(mMenu_Exit);
    }

    /** 注册监听器 */
    private void initListener() {
        mMenu_Message.setOnClickListener(this);
        mMenu_Call.setOnClickListener(this);
        mMenu_Set.setOnClickListener(this);
        mMenu_Whitelist.setOnClickListener(this);
        mMenu_Sensitive.setOnClickListener(this);
        mMenu_Exit.setOnClickListener(this);
    }

    /** 点击事件 */
    public void onClick(View view) {
        switch (((View) view.getParent()).getId()) {
            case R.id.button_message:
                mIntent = new Intent(MainActivity.this, MessageActivity.class);
                MainActivity.this.startActivity(mIntent);
                break;
            case R.id.button_call:
                mIntent = new Intent(MainActivity.this, CallActivity.class);
                MainActivity.this.startActivity(mIntent);
                break;
            case R.id.button_sensitive:
                mIntent = new Intent(MainActivity.this, SensitiveActivity.class);
                MainActivity.this.startActivity(mIntent);
                break;
            case R.id.button_whitelist:
                mIntent = new Intent(MainActivity.this, WhitelistActivity.class);
                MainActivity.this.startActivity(mIntent);
                break;
            case R.id.button_settings:
                mIntent = new Intent(MainActivity.this, SetActivity.class);
                MainActivity.this.startActivity(mIntent);
                break;
            case R.id.button_exit:
                Log.d(TAG, "onClick: stopping srvice");
                exitService = true;
                MainActivity.this.finish();
                break;
        }
    }
}
