package com.comp3050.hearthealthmonitor.activity;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.database.DBHelper;
import com.comp3050.hearthealthmonitor.utility.C_Database;
import com.comp3050.hearthealthmonitor.utility.C_Parameter;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/** Data Chart Activity
 *  Display the heart rate and blood pressure in chart
 **/

public class DataChartActivity extends AppCompatActivity {

    private boolean isHeartRate;
    private Intent result;
    private LineChartView chartView;
    private ActionBar actionBar;
    private long timeStamp;
    private Toast toast;

    private static final int AXIS_TEXT_SIZE = 14;
    private static final int POINT_RADIUS = 5;
    private static final int STROKE_WIDTH = 1;
    private static final int CHART_PADDING = 8;
    private static final int MAX_ZOOM = 20;
    private static final int SECONDS_PER_DAY = 86400;

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_chart);

        if ((timeStamp = getIntent().getLongExtra("data", -1)) == -1) {
            Calendar calendar = Calendar.getInstance();
            timeStamp = setTimeStamp(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        }
        toast = Toast.makeText(this, null, Toast.LENGTH_SHORT);
        chartView = new LineChartView(this);
        chartView.setId(View.generateViewId());
        chartView.setPadding(CHART_PADDING,CHART_PADDING,CHART_PADDING,CHART_PADDING);
        chartView.setMaxZoom(MAX_ZOOM);
        chartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        chartView.setZoomType(ZoomType.HORIZONTAL);
        actionBar = getSupportActionBar();
        result = new Intent(this, DataListActivity.class);
        isHeartRate = true;
        initRadioGroup();
        showAlertDialog();
        updateChart();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_data_chart, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                result.putExtra("data", timeStamp);
                setResult(RESULT_OK, result);
                finish();
                return true;
            case R.id.choose_date:
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(timeStamp * 1000);
                new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        timeStamp = setTimeStamp(year, month, dayOfMonth);
                        result.putExtra("data", timeStamp);
                        setResult(RESULT_OK, result);
                        updateChart();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE)).show();
                break;
            case R.id.switch_view:
                isHeartRate = !isHeartRate;
                showAlertDialog();
                updateChart();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateChart() {

        new Runnable() {
            @Override
            public void run() {
                updateBarTitle();
                try {
                    SQLiteDatabase db = new DBHelper(DataChartActivity.this).getReadableDatabase();
                    Cursor cursor = db.query(DBHelper.TABLE_NAME_DATA, null,
                            C_Database.TIMESTAMP + " >=? AND " + C_Database.TIMESTAMP + " <?",
                            new String[]{String.valueOf(timeStamp), String.valueOf(timeStamp + SECONDS_PER_DAY)},
                            null, null, C_Database.TIMESTAMP);
                    setChart(cursor);
                    cursor.close();
                    db.close();
                } catch (SQLiteException ignored) {}
            }
        }.run();
    }

    private void setChart(Cursor cursor) {

        ConstraintLayout constraintLayout = findViewById(R.id.chartLayout);
        constraintLayout.removeView(chartView);
        View view = findViewById(R.id.textView_default);
        findViewById(R.id.radioGroup).setVisibility(View.GONE);
        findViewById(R.id.legend_1).setVisibility(View.INVISIBLE);
        findViewById(R.id.legend_2).setVisibility(View.INVISIBLE);
        if (!cursor.moveToFirst()) {
            view.setVisibility(View.VISIBLE);
            return;
        }
        view.setVisibility(View.INVISIBLE);

        constraintLayout.addView(chartView);
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        List<Line> lines = new ArrayList<>();
        List<AxisValue> axisXValues = new ArrayList<>();
        Axis axisY = new Axis().setTextSize(AXIS_TEXT_SIZE).setTextColor(Color.BLACK);

        if (isHeartRate) {

            axisY.setName(getString(R.string.text_hr));
            List<PointValue> forBackgroundColor = new ArrayList<>();
            List<PointValue> hr_upper = new ArrayList<>();
            List<PointValue> hr_lower = new ArrayList<>();
            List<PointValue> values = new ArrayList<>();

            int index = 0;
            do {
                int heart_rate = cursor.getInt(cursor.getColumnIndex(C_Database.HEART_RATE));
                if (heart_rate > C_Parameter.HR_LOWER_BOUND && heart_rate < C_Parameter.HR_UPPER_BOUND) {
                    calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000);
                    String label = simpleDateFormat.format(calendar.getTime());
                    axisXValues.add(new AxisValue(++index).setLabel(label));
                    values.add(new PointValue(index, heart_rate).setLabel(label + " " + heart_rate));
                    hr_upper.add(new PointValue(index, C_Parameter.HR_TOO_FAST));
                    hr_lower.add(new PointValue(index, C_Parameter.HR_TOO_SLOW));
                    forBackgroundColor.add(new PointValue(index, C_Parameter.HR_UPPER_BOUND));
                }
            } while (cursor.moveToNext());

            lines.add(new Line(forBackgroundColor).setColor(Color.RED).setCubic(false).setFilled(true).
                    setHasLabels(false).setHasPoints(false).setStrokeWidth(0));
            lines.add(new Line(hr_upper).setColor(Color.WHITE).setAreaTransparency(255).setCubic(false).
                    setFilled(true).setHasLabels(false).setHasPoints(false).setStrokeWidth(0));
            lines.add(new Line(hr_lower).setColor(Color.RED).setCubic(false).
                    setFilled(true).setHasLabels(false).setHasPoints(false).setStrokeWidth(0));
            lines.add(new Line(values).setColor(Color.BLUE).setCubic(false).setFilled(true).
                    setHasLabelsOnlyForSelected(true).setPointRadius(POINT_RADIUS).setStrokeWidth(STROKE_WIDTH));
        }
        else {

            axisY.setName(getString(R.string.text_bp));
            List<PointValue> sbp_values = new ArrayList<>();
            List<PointValue> dbp_values = new ArrayList<>();
            List<PointValue> sbp_prehypertension = new ArrayList<>();
            List<PointValue> sbp_hypertension = new ArrayList<>();
            List<PointValue> dbp_prehypertension = new ArrayList<>();
            List<PointValue> dbp_hypertension = new ArrayList<>();

            int index = 0;
            do {
                int sbp = cursor.getInt(cursor.getColumnIndex(C_Database.SYSTOLIC_BLOOD_PRESSURE));
                int dbp = cursor.getInt(cursor.getColumnIndex(C_Database.DIASTOLIC_BLOOD_PRESSURE));
                if (sbp >= C_Parameter.BP_LOWER_BOUND && dbp >= C_Parameter.BP_LOWER_BOUND) {
                    calendar.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(C_Database.TIMESTAMP)) * 1000);
                    String label = simpleDateFormat.format(calendar.getTime());
                    axisXValues.add(new AxisValue(++index).setLabel(label));
                    sbp_values.add(new PointValue(index, sbp).setLabel(label + " " + sbp));
                    dbp_values.add(new PointValue(index, dbp).setLabel(label + " " + sbp));
                    sbp_prehypertension.add(new PointValue(index, C_Parameter.SBP_PREHYPERTENSION));
                    sbp_hypertension.add(new PointValue(index, C_Parameter.SBP_HYPERTENSION));
                    dbp_prehypertension.add(new PointValue(index, C_Parameter.DBP_PREHYPERTENSION));
                    dbp_hypertension.add(new PointValue(index, C_Parameter.DBP_HYPERTENSION));
                }
            } while (cursor.moveToNext());

            lines.add(new Line(sbp_values).setColor(Color.RED).setCubic(false).setFilled(false).
                    setHasPoints(true).setHasLabelsOnlyForSelected(true).setPointRadius(POINT_RADIUS).setStrokeWidth(1));
            lines.add(new Line(dbp_values).setColor(Color.BLUE).setCubic(false).setFilled(false).
                    setHasPoints(true).setHasLabelsOnlyForSelected(true).setPointRadius(POINT_RADIUS).setStrokeWidth(1));

            lines.add(new Line(sbp_prehypertension).setColor(Color.RED).setCubic(false).setFilled(false).
                    setHasLabels(false).setHasPoints(false).setStrokeWidth(STROKE_WIDTH));
            lines.add(new Line(dbp_prehypertension).setColor(Color.BLUE).setCubic(false).setFilled(false).
                    setHasLabels(false).setHasPoints(false).setStrokeWidth(STROKE_WIDTH));

            lines.add(new Line(sbp_hypertension).setColor(Color.RED).setCubic(false).setFilled(false).setHasLines(false).
                    setHasLabels(false).setHasPoints(false).setStrokeWidth(STROKE_WIDTH));
            lines.add(new Line(dbp_hypertension).setColor(Color.BLUE).setCubic(false).setFilled(false).setHasLines(false).
                    setHasLabels(false).setHasPoints(false).setStrokeWidth(STROKE_WIDTH));

        }

        LineChartData chartData = new LineChartData(lines);
        Axis axisX = new Axis().setName(getString(R.string.text_time)).setTextSize(AXIS_TEXT_SIZE).setTextColor(Color.BLACK)
                .setValues(axisXValues).setMaxLabelChars(AXIS_TEXT_SIZE).setHasSeparationLine(true);
        chartData.setAxisXBottom(axisX);
        chartData.setAxisYLeft(axisY);
        chartView.setLineChartData(chartData);

        setupViewport();
        adjustLayout(constraintLayout);

        TextView legend_1 = findViewById(R.id.legend_1);
        TextView legend_2 = findViewById(R.id.legend_2);
        if (isHeartRate)
            legend_2.setText(R.string.legend_your_hr);
        else {
            RadioGroup radioGroup = findViewById(R.id.radioGroup);
            radioGroup.setVisibility(View.VISIBLE);
            radioGroup.check(R.id.radio_prehypertension);
            legend_1.setVisibility(View.VISIBLE);
            legend_2.setText(R.string.legend_dbp);
        }
        legend_2.setVisibility(View.VISIBLE);
    }

    private void showAlertDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.button_dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        if (isHeartRate) {
            builder.setTitle(R.string.text_hr_info).setMessage(R.string.text_hr_alert);
            builder.create().show();
        }
        else {
            builder.setTitle(R.string.text_bp_info).setMessage(R.string.text_bp_alert_1);
            final AlertDialog alertDialog = builder.create();
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.button_next_page), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });
            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String state = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString();
                    if (state.equals(getString(R.string.button_next_page))) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.button_prev_page);
                        alertDialog.setMessage(getString(R.string.text_bp_alert_2));
                    } else if (state.equals(getString(R.string.button_prev_page))) {
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.button_next_page);
                        alertDialog.setMessage(getString(R.string.text_bp_alert_1));
                    }

                }
            });
        }
    }

    private void updateBarTitle() {
        if (actionBar != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(timeStamp * 1000);
            if (isHeartRate)
                actionBar.setTitle(getString(R.string.label_hr_chart) + " - " +
                        new SimpleDateFormat("yyyy/MM/dd").format(c.getTime()));
            else
                actionBar.setTitle(getString(R.string.label_bp_chart) + " - " +
                        new SimpleDateFormat("yyyy/MM/dd").format(c.getTime()));
        }
    }

    private void adjustLayout(ConstraintLayout constraintLayout) {
        ConstraintSet set= new ConstraintSet();
        int id = chartView.getId();
        set.clone(constraintLayout);
        if (isHeartRate) {
            set.connect(id, ConstraintSet.TOP, R.id.chartLayout, ConstraintSet.TOP);
        }
        else
            set.connect(id, ConstraintSet.TOP, R.id.radioGroup, ConstraintSet.BOTTOM);
        set.connect(id, ConstraintSet.BOTTOM, R.id.chartLayout, ConstraintSet.BOTTOM);
        set.connect(id, ConstraintSet.RIGHT, R.id.chartLayout, ConstraintSet.RIGHT);
        set.connect(id, ConstraintSet.LEFT, R.id.chartLayout, ConstraintSet.LEFT);
        set.constrainHeight(id, ConstraintSet.MATCH_CONSTRAINT);
        set.constrainWidth(id, ConstraintSet.MATCH_CONSTRAINT);
        set.applyTo(constraintLayout);
    }

    private void initRadioGroup() {
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                LineChartData lineChartData = chartView.getLineChartData();
                List<Line> lines = lineChartData.getLines();
                String acknowledgement = "";
                switch (checkedId) {
                    case R.id.radio_prehypertension:
                        acknowledgement = getString(R.string.toast_prehypertension) + "\n" +
                                getString(R.string.text_systolic_bp)+ " >= " + C_Parameter.SBP_PREHYPERTENSION + "\n" +
                                getString(R.string.text_diastolic_bp)+ " >= " + C_Parameter.DBP_PREHYPERTENSION;
                        lines.get(2).setHasLines(true);
                        lines.get(3).setHasLines(true);
                        lines.get(4).setHasLines(false);
                        lines.get(5).setHasLines(false);
                        break;
                    case R.id.radio_hypertension:
                        acknowledgement = getString(R.string.toast_hypertension) + "\n" +
                                getString(R.string.text_systolic_bp)+ " >= " + C_Parameter.SBP_HYPERTENSION + "\n" +
                                getString(R.string.text_diastolic_bp)+ " >= " + C_Parameter.DBP_HYPERTENSION;
                        lines.get(2).setHasLines(false);
                        lines.get(3).setHasLines(false);
                        lines.get(4).setHasLines(true);
                        lines.get(5).setHasLines(true);
                        break;
                }
                toast.setText(acknowledgement);
                toast.show();
                lineChartData.setLines(lines);
                chartView.setLineChartData(lineChartData);
                setupViewport();
            }
        });
    }

    private void setupViewport() {
        Viewport viewport = new Viewport(chartView.getMaximumViewport());
        if (isHeartRate) {
            viewport.bottom = C_Parameter.HR_LOWER_BOUND;
            viewport.top = C_Parameter.HR_UPPER_BOUND;
        } else {
            viewport.bottom = C_Parameter.BP_LOWER_BOUND;
            viewport.top = C_Parameter.BP_UPPER_BOUND;
        }
        chartView.setMaximumViewport(viewport);
        viewport.left = 0;
        viewport.right = 20;
        chartView.setCurrentViewport(viewport);
    }

    private long setTimeStamp(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth, 0, 0, 0);
        return calendar.getTimeInMillis()/1000;
    }
}
