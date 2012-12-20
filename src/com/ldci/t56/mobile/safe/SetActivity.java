
package com.ldci.t56.mobile.safe;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ldci.t56.mobile.tool.BroadCastTool;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SetActivity extends ListActivity {
    private static final String TAG = "SetActivity";
    private SharedPreferences mSharedPreferences;
    private String mUndisturbedContent;
    public static String mStartTime = "22:00";
    public static String mEndTime = "08:00";
    Calendar mCalendar;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        Log.d("debug", "onCreate");
        mCalendar = Calendar.getInstance();
        /* 读取用SharedPreferences保存的配置文件数据 */
        mSharedPreferences = this.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);

        MainActivity.mUndisturbedMode = mSharedPreferences.getInt("UndisturbedMode",
                MainActivity.mUndisturbedMode);
        switch (MainActivity.mUndisturbedMode) {
            case 0:
                mUndisturbedContent = getString(R.string.close);
                break;
            case 1:
                mUndisturbedContent = getString(R.string.intercept_sms);
                break;
            case 2:
                mUndisturbedContent = getString(R.string.intercept_tel);
                break;
            case 3:
                mUndisturbedContent = getString(R.string.intercept_both);
                break;
        }

        long timeInMinute = mSharedPreferences.getLong("UndisturbedStartTime", 1320);
        if (timeInMinute > 48 * 60) {
            mCalendar.setTimeInMillis(timeInMinute);
            timeInMinute = mCalendar.get(Calendar.HOUR_OF_DAY) * 60
                    + mCalendar.get(Calendar.MINUTE);
        }
        int hourS = (int) (timeInMinute / 60);
        int minuteS = (int) (timeInMinute % 60);
        mStartTime = (hourS < 10 ? "0" + hourS : hourS) + ":"
                + (minuteS < 10 ? "0" + minuteS : minuteS);

        timeInMinute = mSharedPreferences.getLong("UndisturbedEndTime", 480);
        if (timeInMinute > 48 * 60) {
            mCalendar.setTimeInMillis(timeInMinute);
            timeInMinute = mCalendar.get(Calendar.HOUR_OF_DAY) * 60
                    + mCalendar.get(Calendar.MINUTE);
        }
        int hourE = (int) ((timeInMinute / 60) % 24);
        int minuteE = (int) (timeInMinute % 60);
        mEndTime = (hourE < 10 ? "0" + hourE : hourE) + ":"
                + (minuteE < 10 ? "0" + minuteE : minuteE);

        MainActivity.isCheckBoxChecked[3] = mSharedPreferences.getBoolean("isAutoStartWithPhone",
                false);
        /* 读取结束后开始适配列表数据 */
        setListAdapter(new SetAdapter(SetActivity.this));
        initListViewListener();
    }

    @Override
    protected void onDestroy() {

        mSharedPreferences = this.getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("UndisturbedMode", MainActivity.mUndisturbedMode);

        int hourStart = Integer.valueOf(mStartTime.split(":")[0]);
        int minuteStart = Integer.valueOf(mStartTime.split(":")[1]);
        int hourEnd = Integer.valueOf(mEndTime.split(":")[0]);
        int minuteEnd = Integer.valueOf(mEndTime.split(":")[1]);
        int minutes = hourStart * 60 + minuteStart;
        editor.putLong("UndisturbedStartTime", minutes);
        Log.d(TAG, "UndisturbedStartTime:" + minutes);
        if (hourStart > hourEnd) {
            hourEnd += 24;
        }
        minutes = hourEnd * 60 + minuteEnd;
        editor.putLong("UndisturbedEndTime", minutes);
        Log.d(TAG, "UndisturbedEndTime:" + minutes);

        editor.putBoolean("isAutoStartWithPhone", MainActivity.isCheckBoxChecked[3]);
        editor.commit();
        Intent intent = new Intent(BroadCastTool.UPDATE_PREFERENCE_ACTION);
        intent.putExtra(BroadCastTool.ENABLE_PROTECTION, true);
        sendBroadcast(intent);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    /** ListView的点击事件监听 */
    private void initListViewListener() {
        getListView().setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View view, int position,
                    long arg3) {
                CheckBox mCheckBox = (CheckBox) view.findViewById(R.id.set_checkbox);
                if (!mCheckBox.isChecked()) {
                    MainActivity.isCheckBoxChecked[position] = true;
                    mCheckBox.setChecked(true);
                } else {
                    MainActivity.isCheckBoxChecked[position] = false;
                    mCheckBox.setChecked(false);
                }
                switch (position) {
                    case 0:
                        initUnDisturbedDialog();
                        break;
                    case 1:
                        String startTime[] = mStartTime.split(":");
                        setUndisturbedTime(1, startTime[0], startTime[1]);
                        break;
                    case 2:
                        String endTime[] = mEndTime.split(":");
                        setUndisturbedTime(2, endTime[0], endTime[1]);
                        break;
                    case 3:
                        mSharedPreferences.edit().putBoolean("isAutoStartWithPhone",
                                MainActivity.isCheckBoxChecked[position]).commit();
                        break;
                }
            }

        });
    }


    String hourStr;
    String minuteStr;

    /** 时间设置 */
    private void setUndisturbedTime(final int start_or_end, String hour, String minute) {

        new TimePickerDialog(SetActivity.this, new TimePickerDialog.OnTimeSetListener() {

            public void onTimeSet(TimePicker view, int hour,
                    int minute) {
                String startTime[] = mStartTime.split(":");
                String endTime[] = mEndTime.split(":");
                if (start_or_end == 1) {
                    hourStr = (hour < 10 ? "0" + hour : "" + hour);
                    minuteStr = (minute < 10 ? "0" + minute : "" + minute);
                    if (hourStr.equals(endTime[0]) && minuteStr.equals(endTime[1])) {
                        Toast.makeText(getApplicationContext(), R.string.input_time_same_error,
                                Toast.LENGTH_SHORT)
                                .show();
                        setUndisturbedTime(1, startTime[0], startTime[1]);
                    } else {
                        mStartTime = hourStr + ":" + minuteStr;
                    }
                } else {
                    hourStr = (hour < 10 ? "0" + hour : "" + hour);
                    minuteStr = (minute < 10 ? "0" + minute : "" + minute);
                    if (startTime[0].equals(hourStr) && startTime[1].equals(minuteStr)) {
                        Toast.makeText(getApplicationContext(), R.string.input_time_same_error,
                                Toast.LENGTH_SHORT)
                                .show();
                        setUndisturbedTime(2, endTime[0], endTime[1]);
                    } else {
                        mEndTime = hourStr + ":" + minuteStr;
                    }
                }
                setListAdapter(new SetAdapter(SetActivity.this));
            }

        }, Integer.valueOf(hour), Integer.valueOf(minute), true).show();
    }

    /** 夜间免扰模式选择框，包含拦截短信，拦截电话，拦截短信和电话以及关闭 */
    private void initUnDisturbedDialog() {
        LayoutInflater mLI = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout mLL = (LinearLayout) mLI.inflate(R.layout.set_undisturbed, null);
        final RadioGroup mRG = (RadioGroup) mLL.findViewById(R.id.RadioGroup01);
        final RadioButton mRB1 = (RadioButton) mLL.findViewById(R.id.RadioButton01);
        final RadioButton mRB2 = (RadioButton) mLL.findViewById(R.id.RadioButton02);
        final RadioButton mRB3 = (RadioButton) mLL.findViewById(R.id.RadioButton03);
        final RadioButton mRB4 = (RadioButton) mLL.findViewById(R.id.RadioButton04);

        switch (MainActivity.mUndisturbedMode) {
            case 0:
                mRB4.setChecked(true);
                break;
            case 1:
                mRB1.setChecked(true);
                break;
            case 2:
                mRB2.setChecked(true);
                break;
            case 3:
                mRB3.setChecked(true);
                break;
        }
        RadioGroup.OnCheckedChangeListener mOCCL = new RadioGroup.OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                if (arg1 == mRB1.getId()) {
                    mUndisturbedContent = getString(R.string.intercept_sms);
                    MainActivity.mUndisturbedMode = 1;
                } else if (arg1 == mRB2.getId()) {
                    mUndisturbedContent = getString(R.string.intercept_tel);
                    MainActivity.mUndisturbedMode = 2;
                } else if (arg1 == mRB3.getId()) {
                    mUndisturbedContent = getString(R.string.intercept_both);
                    MainActivity.mUndisturbedMode = 3;
                } else if (arg1 == mRB4.getId()) {
                    mUndisturbedContent = getString(R.string.close);
                    MainActivity.mUndisturbedMode = 0;
                }
            }

        };
        mRG.setOnCheckedChangeListener(mOCCL);

        new AlertDialog.Builder(this).setTitle(R.string.intercept_content_title).setView(mLL)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        setListAdapter(new SetAdapter(SetActivity.this));
                    }
                }).show();
    }

    /** 自定义适配器 */
    class SetAdapter extends BaseAdapter {

        private ArrayList<Map<String, String>> mList = new ArrayList<Map<String, String>>();
        private Map<String, String> mMap;
        private String[] mTitle = {
                mContext.getString(R.string.night_scrambling),
                mContext.getString(R.string.start_time),
                mContext.getString(R.string.end_time),
                mContext.getString(R.string.start_on_boot)
        };
        private String[] mContent = {
                mUndisturbedContent,
                mStartTime,
                mEndTime,
                mContext.getString(R.string.start_on_boot_summary)
        };

        public SetAdapter(Context context) {
            Log.d("debug", "SetAdapter");
            initListData();
        }

        public void initListData() {
            for (int i = 0; i < mTitle.length; i++) {
                mMap = new HashMap<String, String>();
                mMap.put("set_title", mTitle[i]);
                mMap.put("set_content", mContent[i]);
                mList.add(mMap);
            }
        }

        public int getCount() {
            return mList.size();
        }

        public Object getItem(int position) {
            return mList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View view, ViewGroup viewgroup) {
            LayoutInflater mLI = (LayoutInflater) SetActivity.this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            RelativeLayout mLL = (RelativeLayout) mLI.inflate(R.layout.set_listview, null);
            TextView mTitle = (TextView) mLL.findViewById(R.id.set_title);
            TextView mContent = (TextView) mLL.findViewById(R.id.set_content);
            final CheckBox mCheckBox = (CheckBox) mLL.findViewById(R.id.set_checkbox);
            mTitle.setText(mList.get(position).get("set_title"));
            mContent.setText(mList.get(position).get("set_content"));
            mCheckBox.setFocusable(false);// 取消CheckBox的焦点
            mCheckBox.setEnabled(false);
            if (MainActivity.isCheckBoxChecked[position] == true) {
                mCheckBox.setChecked(true);
            } else {
                mCheckBox.setChecked(false);
            }
            if (position == 0 || position == 1 || position == 2) {
                mCheckBox.setVisibility(View.GONE);
            }
            return mLL;
        }
    }

}
