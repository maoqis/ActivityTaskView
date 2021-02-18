package cc.rome753.demo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rome753@163.com on 2017/3/31.
 */

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    public CheckBox mCheckboxFinish;
    String[] NAMES = {
            "BROUGHT_TO_FRONT",
            "CLEAR_TASK",
            "CLEAR_TOP",
            "CLEAR_WHEN_TASK_RESET",
            "EXCLUDE_FROM_RECENTS",
            "FORWARD_RESULT",
            "LAUNCHED_FROM_HISTORY",
            "LAUNCH_ADJACENT",
            "MULTIPLE_TASK",
            "NEW_DOCUMENT",
            "NEW_TASK",
            "NO_ANIMATION",
            "NO_HISTORY",
            "NO_USER_ACTION",
            "PREVIOUS_IS_TOP",
            "REORDER_TO_FRONT",
            "RESET_TASK_IF_NEEDED",
            "RETAIN_IN_RECENTS",
            "SINGLE_TOP",
            "TASK_ON_HOME",
    };

    int[] FLAGS = {
            Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            Intent.FLAG_ACTIVITY_CLEAR_TASK,
            Intent.FLAG_ACTIVITY_CLEAR_TOP,
            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET,
            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            Intent.FLAG_ACTIVITY_FORWARD_RESULT,
            Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
            Intent.FLAG_ACTIVITY_NEW_DOCUMENT,
            Intent.FLAG_ACTIVITY_NEW_TASK,
            Intent.FLAG_ACTIVITY_NO_ANIMATION,
            Intent.FLAG_ACTIVITY_NO_HISTORY,
            Intent.FLAG_ACTIVITY_NO_USER_ACTION,
            Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP,
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT,
            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED,
            Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS,
            Intent.FLAG_ACTIVITY_SINGLE_TOP,
            Intent.FLAG_ACTIVITY_TASK_ON_HOME,
    };

    private LinearLayout llContainer;
    private LinearLayout mLlContainer;
    private Integer mSelectTaskId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    protected void init() {
        setContentView(R.layout.activity_base);
        initView();

        setTitle(getClass().getSimpleName());
        setActionBarBack();
        addCheckBoxes();
    }

    private void initView() {
        mLlContainer = (LinearLayout) findViewById(R.id.ll_container);
        mCheckboxFinish = (CheckBox) findViewById(R.id.checkbox_finish);

    }

    private void setActionBarBack() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    protected void addCheckBoxes() {
        llContainer = (LinearLayout) findViewById(R.id.ll_container);
        for (String name : NAMES) {
            CheckBox cb = new CheckBox(this);
            cb.setText(name);
            llContainer.addView(cb);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void addFlags(Intent intent) {
        for (int i = 0; i < llContainer.getChildCount(); i++) {
            CheckBox cb = (CheckBox) llContainer.getChildAt(i);
            if (cb.isChecked()) {
                intent.addFlags(FLAGS[i]);
            }
        }
    }

    public void startFragmentActivity(View v) {
        Intent intent = new Intent(this, DemoFragmentActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startDialogActivity(View v) {
        Intent intent = new Intent(this, DialogActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startMainActivity(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startIntentActivity(View v) {
        Intent intent = new Intent(this, IntentActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startStandard(View v) {
        Intent intent = new Intent(this, StandardActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startSingleTop(View v) {
        Intent intent = new Intent(this, SingleTopActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startSingleTask(View v) {
        Intent intent = new Intent(this, SingleTaskActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void startSingleInstance(View v) {
        Intent intent = new Intent(this, SingleInstanceActivity.class);
        addFlags(intent);
        startActivity(intent);
    }

    public void selectTask(View v) {
        Log.d(TAG, "selectTask() called with: v = [" + v + "]");
        Button textView = (Button) v;
        ActivityManager activityManager = this.getSystemService(ActivityManager.class);
        List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        Log.d(TAG, "selectTask: appTasks.size=" + appTasks.size());
        List<String> list = getTaskInfoStringArray(appTasks);
        list.add(0, "无指定task");
        String[] strings = new String[list.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = list.get(i);
        }
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(strings, -1, (dialog, which) -> {

                    if (which == 0) {
                        mSelectTaskId = null;
                        textView.setText(null);
                        return;
                    }

                    ActivityManager.AppTask appTask = appTasks.get(which - 1);
                    ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                    String text = v.getResources().getString(R.string.select_task) + ":"
                            + Integer.toHexString(getTaskId(taskInfo)) + "\n* 将从task中启动activity";
                    textView.setText(text);
                    mSelectTaskId = getTaskId(taskInfo);
                }).show();

    }

    public void frontTask(View v) {
        if (mSelectTaskId == null) {
            Toast.makeText(v.getContext(), getText(R.string.please_select_task), Toast.LENGTH_LONG);

        } else {
            ActivityManager activityManager = this.getSystemService(ActivityManager.class);
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
            for (ActivityManager.AppTask appTask : appTasks) {
                ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                int taskId = getTaskId(taskInfo);
                if (taskId == mSelectTaskId) {
                    appTask.moveToFront();
                    break;
                }

                Toast.makeText(v.getContext(), getText(R.string.not_found_task) + "：" + Integer.toHexString(getTaskId(taskInfo)), Toast.LENGTH_SHORT);

            }
        }

    }


    @Override
    public void startActivity(Intent intent) {

        if (!handleTaskStart(intent)) {
            super.startActivity(intent);
            Log.d(TAG, "startActivity: super");
        }


        if (mCheckboxFinish == null) {
            Log.d(TAG, "startActivity: mCheckboxFinish null");
            return;
        }
        boolean checked = mCheckboxFinish.isChecked();
        Log.d(TAG, "startActivity: checked=" + checked);
        if (mCheckboxFinish.isChecked()) {
            finish();
        }

    }

    private boolean handleTaskStart(Intent intent) {
        if (mSelectTaskId != null) {
            ActivityManager activityManager = this.getSystemService(ActivityManager.class);
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
            for (int i = 0; i < appTasks.size(); i++) {
                ActivityManager.AppTask appTask = appTasks.get(i);
                ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
                if (getTaskId(taskInfo) == mSelectTaskId) {
                    Log.d(TAG, "handleTaskStart() called");
                    appTask.startActivity(this, intent, null);
                    return true;
                }
            }
        }
        return false;
    }

    private int getTaskId(ActivityManager.RecentTaskInfo taskInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return taskInfo.taskId;
        } else {
            return taskInfo.id;
        }
    }

    private List<String> getTaskInfoStringArray(List<ActivityManager.AppTask> appTasks) {
        ArrayList ret = new ArrayList<String>();
        for (int i = 0; i < appTasks.size(); i++) {
            ActivityManager.AppTask appTask = appTasks.get(i);
            ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
            String s = "TaskInfo---0x" + Integer.toHexString(getTaskId(taskInfo)) + "\n"
                    + " taskId=" + getTaskId(taskInfo) + "\n"
                    + " baseIntent=" + taskInfo.baseIntent + "\n"
                    + " origActivity=" + taskInfo.origActivity;

            Log.d(TAG, "selectTask: flatMap s=" + s);
            ret.add(s);
        }
        return ret;

    }


}
