package com.comp3050.hearthealthmonitor.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.utility.C_Database;
import com.comp3050.hearthealthmonitor.utility.C_Parameter;

public class DataListActivity extends AppCompatActivity {

    private static final int REQUEST_CHANGED_DATE = 0x0a;
    private ListView listView;
    private ActionBar actionBar;
    private long timeStamp = -1;
    private boolean indicateAbnormal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        listView = findViewById(R.id.list);
        actionBar = getSupportActionBar();
    }

    @Override
    protected void onResume() {
        updateList();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_data_list, menu);
        return true;
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, DataChartActivity.class);
        intent.putExtra("data", timeStamp);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.choose_date:
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(timeStamp * 1000);
                new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        timeStamp = setTimeStamp(year, month, dayOfMonth);
                        updateList();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE)).show();
                break;
            case R.id.view_chart:
                startActivityForResult(intent, REQUEST_CHANGED_DATE);
                break;
            case R.id.view_abnormal:
                indicateAbnormal = !indicateAbnormal;
                item.setChecked(indicateAbnormal);
                if (indicateAbnormal) {
                    final AlertDialog alertDialog = new AlertDialog.Builder(this).
                            setTitle(R.string.text_info).setMessage(R.string.text_list_legend).
                            setNegativeButton(R.string.button_dismiss, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == AlertDialog.BUTTON_NEGATIVE)
                                        dialog.dismiss();
                                }
                            }).create();
                    alertDialog.setCanceledOnTouchOutside(false);
                    alertDialog.setView(LayoutInflater.from(this).inflate(R.layout.dialog_legend, null));
                    alertDialog.show();
                }
                updateList();
                break;
            case R.id.refresh_list:
                updateList();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHANGED_DATE && resultCode == RESULT_OK && data != null) {
            long changedDate = data.getLongExtra("data", -1);
            if (changedDate != -1 && changedDate != timeStamp) {
                timeStamp = changedDate;
                updateList();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateList() {

        if (actionBar != null) {
            Calendar c = Calendar.getInstance();
            if (timeStamp != -1)
                c.setTimeInMillis(timeStamp * 1000);
            else
                timeStamp = setTimeStamp(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE));
            actionBar.setTitle(getString(R.string.label_data) + " - " +
                    new SimpleDateFormat("yyyy/MM/dd").format(c.getTime()));
        }
        try {
            SQLiteDatabase db = new DBHelper(this).getReadableDatabase();
            Cursor cursor = db.query(DBHelper.TABLE_NAME_DATA, null,
                    C_Database.TIMESTAMP + " >=? AND " + C_Database.TIMESTAMP + " <?",
                    new String[] { String.valueOf(timeStamp), String.valueOf(timeStamp + 86400) },
                    null, null, C_Database.TIMESTAMP + " DESC");

            View view = findViewById(R.id.textView_default);
            if (!cursor.moveToFirst()) {
                view.setVisibility(View.VISIBLE);
                listView.setAdapter(null);
            } else {
                view.setVisibility(View.INVISIBLE);
                listView.setAdapter(new MyCursorAdapter(this,
                        R.layout.list_view_band,
                        cursor, new String[]{ C_Database.STEPS, C_Database.HEART_RATE },
                        new int[]{ R.id.list_item_steps, R.id.list_item_hr }, indicateAbnormal));
            }
            db.close();
        } catch (SQLiteException ex) {
            Toast.makeText(this, R.string.toast_db_updating, Toast.LENGTH_SHORT).show();
        }
    }

    private long setTimeStamp(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        return calendar.getTimeInMillis()/1000;
    }

    private class MyCursorAdapter extends SimpleCursorAdapter {

        private Calendar calendar;
        private boolean indicateAbnormal;

        MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, boolean flag) {
            super(context, layout, c, from, to, 0);
            this.calendar = Calendar.getInstance();
            this.indicateAbnormal = flag;
        }

        @Override
        public void bindView(View view, final Context context, final Cursor cursor) {
            super.bindView(view, context, cursor);
            final int systolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.SYSTOLIC_BLOOD_PRESSURE));
            final int diastolicBP = cursor.getInt(cursor.getColumnIndex(C_Database.DIASTOLIC_BLOOD_PRESSURE));
            final String blood_pressure = systolicBP + "/" + diastolicBP;
            calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000);
            TextView textView = view.findViewById(R.id.list_item_time);
            textView.setText(new SimpleDateFormat("HH:mm:ss").format(calendar.getTime()));
            textView = view.findViewById(R.id.list_item_bp);
            textView.setText(blood_pressure);

            if (indicateAbnormal) {
                if (systolicBP >= C_Parameter.SBP_HYPERTENSIVE_CRISIS || diastolicBP >= C_Parameter.DBP_HYPERTENSIVE_CRISIS)
                    textView.setTextColor(ListTextColors.HBP_CRISIS);
                else if (systolicBP >= C_Parameter.SBP_HYPERTENSIVE_STAGE2 || diastolicBP >= C_Parameter.DBP_HYPERTENSIVE_STAGE2)
                    textView.setTextColor(ListTextColors.HBP_STAGE_2);
                else if (systolicBP >= C_Parameter.SBP_HYPERTENSIVE_STAGE1 || diastolicBP >= C_Parameter.DBP_HYPERTENSIVE_STAGE1)
                    textView.setTextColor(ListTextColors.HBP_STAGE_1);
                else
                    textView.setTextColor(ListTextColors.NORMAL);
                final int heart_rate = cursor.getInt(cursor.getColumnIndex(C_Database.HEART_RATE));
                textView = view.findViewById(R.id.list_item_hr);
                if (heart_rate > C_Parameter.HR_TOO_FAST)
                    textView.setTextColor(ListTextColors.HR_TOO_FAST);
                else if (heart_rate < C_Parameter.HR_TOO_SLOW)
                    textView.setTextColor(ListTextColors.HR_TOO_SLOW);
                else
                    textView.setTextColor(ListTextColors.NORMAL);
            }
        }
    }

    private static final class ListTextColors {
        static final int NORMAL = Color.DKGRAY;
        static final int HR_TOO_FAST = Color.RED;
        static final int HR_TOO_SLOW = Color.BLUE;
        static final int HBP_STAGE_1 = Color.BLUE;
        static final int HBP_STAGE_2 = Color.MAGENTA;
        static final int HBP_CRISIS = Color.RED;
    }
}
