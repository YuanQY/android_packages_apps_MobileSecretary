
package com.ldci.t56.mobile.safe;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts.Phones;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ldci.t56.mobile.db.DbAdapter;
import com.ldci.t56.mobile.info.Call_Allow_Info;
import com.ldci.t56.mobile.tool.PhoneNumberUtils;

import java.util.ArrayList;
import java.util.List;

public class WhitelistActivity extends ListActivity {
    private static final String TAG = "WhiteListActivity";
    private List<String> mList = new ArrayList<String>();
    public final int ADD_PHONE = Menu.FIRST;
    public final int DELETE = Menu.FIRST + 1;
    public final int MULTIPLE_SELECTION = Menu.FIRST + 2;
    public static int single_or_multiple = 1;

    private EditText mForbidPhone;
    private String checkInfo;
    private ListView mLV;
    private static String mDuplicatePhoneFormat;
    private Context mContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        mDuplicatePhoneFormat = getString(R.string.duplicate_phone_number);
        initWhiltlistData();
    }

    private void initWhiltlistData() {
        DbAdapter dbAdapter = new DbAdapter(mContext);
        try {
            dbAdapter.open();
            Cursor cursor = dbAdapter.getAll(DbAdapter.CALL_ALLOW_TABLE_NAME);
            int mCount = cursor.getCount();
            if (mCount >= 0) {
                String phoneNumber = null;
                if (cursor.moveToFirst()) {
                    mList.clear();
                    do {
                        phoneNumber = cursor.getString(cursor
                                .getColumnIndex(DbAdapter.CALL_ALLOW_PHONE));
                        mList.add(phoneNumber);
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        } finally {
            dbAdapter.close();
        }
        if (single_or_multiple == 1) {
            singleMode((String[]) mList.toArray(new String[mList.size()]));
        } else if (single_or_multiple == 2) {
            multipleMode((String[]) mList.toArray(new String[mList.size()]));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.addSubMenu(0, ADD_PHONE, 0, R.string.add).setIcon(android.R.drawable.ic_menu_add);
        return super.onCreateOptionsMenu(menu);
    }

    boolean isCreateOptionsMenu = false;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (getListView().getCount() > 0) {
            if (isCreateOptionsMenu == false) {
                menu.addSubMenu(0, DELETE, 0, R.string.delete).setIcon(
                        android.R.drawable.ic_menu_delete);
                menu.addSubMenu(0, MULTIPLE_SELECTION, 0, R.string.mutiple_select).setIcon(
                        android.R.drawable.ic_menu_manage);
                isCreateOptionsMenu = true;
            }
        } else {
            menu.removeItem(DELETE);
            menu.removeItem(MULTIPLE_SELECTION);
            isCreateOptionsMenu = false;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ADD_PHONE:
                single_or_multiple = 1;
                addForbidPhone(null);
                break;
            case DELETE:
                if (single_or_multiple == 1) {
                    String phone = (String) getListView().getSelectedItem();
                    if (phone != null) {
                        DbAdapter dbAdapter = new DbAdapter(mContext);
                        try {
                            dbAdapter.open();
                            dbAdapter.getDel(DbAdapter.CALL_ALLOW_TABLE_NAME,
                                    DbAdapter.CALL_ALLOW_PHONE, phone);
                        } finally {
                            dbAdapter.close();
                        }
                        initWhiltlistData();
                    }
                } else {

                    // -------------------------------------------------------------------------------多选的删除：开始----------------------------------------------------------------
                    Log.d(TAG, "mList size is" + mList.size());
                    Log.d(TAG, "checked item size is "
                            + getListView().getCheckedItemPositions().size());
                    int m = mList.size();
                    DbAdapter dbAdapter = new DbAdapter(mContext);
                    try {
                        dbAdapter.open();
                        for (int i = 0; i != m; i++) {
                            if (getListView().getCheckedItemPositions().get(i)) {
                                Log.d(TAG, "item checked info is inner :" + i + " is "
                                        + getListView().getCheckedItemPositions().get(i));
                                Log.d(TAG, "item content info is :" + i + " is "
                                        + getListView().getAdapter().getItem(i));
                                if (mList.contains(getListView().getAdapter().getItem(i))) {
                                    dbAdapter.getDel(DbAdapter.CALL_ALLOW_TABLE_NAME,
                                            DbAdapter.CALL_ALLOW_PHONE,
                                            getListView().getAdapter().getItem(i).toString());
                                }
                            }
                        }
                    } finally {
                        dbAdapter.close();
                    }
                    Log.d(TAG, "initListData start");
                    initWhiltlistData();
                    Log.d(TAG, "initListData end");
                    // --------------------------------------------------------------------------------多选的删除：结束----------------------------------------------------------------

                }
                break;
            case MULTIPLE_SELECTION:
                single_or_multiple = 2;
                initWhiltlistData();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 单选模式 */
    private void singleMode(String[] mArray) {
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_checked, mArray);
        getListView().setAdapter(mAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    /** 多选模式 */
    private void multipleMode(String[] mArray) {
        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, mArray);
        getListView().setAdapter(mAdapter);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    private void initListData(final String[] contant) {
        AlertDialog mAlertDialog = new AlertDialog.Builder(this).setMultiChoiceItems(contant,
                new boolean[contant.length],
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkInfo = "";
                for (int i = 0; i != contant.length; i++) {
                    if (mLV.getCheckedItemPositions().get(i)) {
                        checkInfo += mLV.getAdapter().getItem(i).toString() + ";";
                    }
                }
                mForbidPhone.setText(checkInfo.substring(0, checkInfo.length()));
            }
        }).setNegativeButton(R.string.cancel, null).create();
        mLV = mAlertDialog.getListView();
        mAlertDialog.show();
    }

    private void showMulitAlertDialog() {
        Cursor cursor = getContentResolver().query(Phones.CONTENT_URI, new String[] {
                Phones._ID, Phones.DISPLAY_NAME, Phones.NUMBER
        }, null, null, Phones.NUMBER + " desc");
        if (cursor.moveToFirst()) {
            int m = 0;
            String contant[] = new String[cursor.getCount()];
            do {
                String phoneNumber = cursor.getString(cursor.getColumnIndex(Phones.NUMBER));
                phoneNumber = phoneNumber.replaceAll(" ", "").replaceAll("-", "");
                contant[m] = cursor.getString(cursor.getColumnIndex(Phones.DISPLAY_NAME)) + ":"
                        + (phoneNumber.length() == 13 ? phoneNumber.substring(2) : phoneNumber);
                m++;
            } while (cursor.moveToNext());
            initListData(contant);
        }
        cursor.close();

    }

    private void addForbidPhone(String phoneNumber) {
        LayoutInflater mLI = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout mRL = (RelativeLayout) mLI.inflate(R.layout.message_forbid_add, null);
        mForbidPhone = (EditText) mRL.findViewById(R.id.new_forbid_phone);
        if (phoneNumber != null) {
            mForbidPhone.setText(PhoneNumberUtils.trimSmsNumber(phoneNumber));
        }
        Button mButton = (Button) mRL.findViewById(R.id.get_system_contact);
        mButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                showMulitAlertDialog();
            }
        });

        new AlertDialog.Builder(WhitelistActivity.this).setTitle(R.string.white_list_title)
                .setView(mRL)
                .setPositiveButton(R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        String mNewForbidPhone = mForbidPhone.getText().toString();
                        if (mNewForbidPhone.length() < 3) {
                            Toast.makeText(WhitelistActivity.this, R.string.input_error,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            String mStr[] = mNewForbidPhone.split(";");
                            DbAdapter dbAdapter = new DbAdapter(mContext);
                            try {
                                dbAdapter.open();
                                for (int i = 0; i != mStr.length; i++) {
                                    String phone = mStr[i].split(":").length == 1 ? mStr[i]
                                            .split(":")[0]
                                            : mStr[i].split(":")[1];

                                    Cursor cursorPhone = dbAdapter.getPhone(phone, 5);
                                    if (!cursorPhone.moveToFirst()) {
                                        Call_Allow_Info mCFI = new Call_Allow_Info();
                                        mCFI.setCall_allow_phone(phone);
                                        dbAdapter.getAdd(mCFI);
                                    } else {
                                        Toast.makeText(WhitelistActivity.this,
                                                String.format(mDuplicatePhoneFormat, phone),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    cursorPhone.close();

                                }
                            }
                            finally {
                                dbAdapter.close();
                            }
                            initWhiltlistData();
                            Toast.makeText(WhitelistActivity.this, R.string.add_contacts_success,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, null).show();
    }
}
