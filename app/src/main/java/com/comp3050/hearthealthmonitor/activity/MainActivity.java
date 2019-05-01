package com.comp3050.hearthealthmonitor.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.comp3050.hearthealthmonitor.R;
import com.comp3050.hearthealthmonitor.service.MonitoringService;

/** Main Activity
 *  The main entry of application
 **/

public class MainActivity extends AppCompatActivity {

    private static final String DOWNLOAD_PAGE = "https://raw.githubusercontent.com/5656hcx/Gadgetbridge/master/app/release/app-release.apk";
    private static final String SOURCE_PACKAGE = "nodomain.freeyourgadget.gadgetbridge";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            if (checkDependency())
                startService(new Intent(this, MonitoringService.class));
        }
    }

    private boolean checkDependency() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(SOURCE_PACKAGE);
        if (intent.resolveActivity(getPackageManager()) == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.toast_dependency_lost
            ).setPositiveButton(R.string.button_download, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(DOWNLOAD_PAGE));
                    startActivity(intent);
                    finish();
                }
            }).setNegativeButton(R.string.button_exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setCancelable(false).show();
            return false;
        }
        return true;
    }

    public void viewOtherActivities(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.button_data:
                intent.setClass(this, DataListActivity.class);
                break;
            case R.id.button_message:
                intent.setClass(this, MessageActivity.class);
                break;
            case R.id.button_development:
                intent.setClass(this, DeveloperActivity.class);
                break;
            case R.id.button_about:
                intent.setClass(this, AboutActivity.class);
                break;
        }
        startActivity(intent);
    }
}
