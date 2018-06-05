package com.hose.aureliano.project.done.activity;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.hose.aureliano.project.done.R;
import com.hose.aureliano.project.done.activity.dialog.DateTimePickerDialog;
import com.hose.aureliano.project.done.model.Task;
import com.hose.aureliano.project.done.service.TaskService;
import com.hose.aureliano.project.done.service.schedule.alarm.AlarmService;
import com.hose.aureliano.project.done.utils.ActivityUtils;
import com.hose.aureliano.project.done.utils.CalendarUtils;

import org.apache.commons.lang3.StringUtils;

public class TaskDetailsActivity extends AppCompatActivity {

    private static int COLOR_BLACK_PRIMARY;
    private static int COLOR_BLACK_SECONDARY;
    private static int COLOR_BLUE;
    private static int COLOR_RED;
    private static Drawable DRAWABLE_DONE;
    private static Drawable DRAWABLE_CANCEL;

    private TaskService taskService = new TaskService(this);

    private ImageView doneCancelButtonIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_details_activity);
        initStaticResources();

        Bundle extras = getIntent().getExtras();
        Task task;
        if (null != extras) {
            task = buildTask(extras);
            task.setListId(extras.getInt("listId"));

            EditText taskNameView = findViewById(R.id.task_details_main_text);
            taskNameView.setText(task.getName());
            prepareTaskName(taskNameView, task.getDone());

            View repeatView = findViewById(R.id.task_details_repeat);
            repeatView.setVisibility(View.GONE);

            View dueDateView = findViewById(R.id.task_details_due_date);
            ImageView dueDateIcon = findViewById(R.id.task_details_due_date_icon);
            TextView dueDateText = findViewById(R.id.task_details_due_date_text);
            View dueDateClearView = findViewById(R.id.task_details_due_date_clear);
            changeDueDateFields(dueDateText, dueDateIcon, repeatView, task);
            dueDateClearView.setOnClickListener(view -> {
                task.setDueDateTime(null);
                repeatView.setVisibility(View.GONE);
                changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
            });
            dueDateView.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(this, view);
                popupMenu.inflate(R.menu.menu_due_date);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_tasks_due_date_today:
                            task.setDueDateTime(CalendarUtils.getTodayDateTimeInMillis());
                            changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
                            break;
                        case R.id.menu_tasks_due_date_tomorrow:
                            task.setDueDateTime(CalendarUtils.getTomorrowDateTimeInMillis());
                            changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
                            break;
                        case R.id.menu_tasks_due_date_next_week:
                            task.setDueDateTime(CalendarUtils.getNextMondayDateTimeInMillis());
                            changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
                            break;
                        case R.id.menu_tasks_select_due_date:
                            ActivityUtils.showDatePickerDialog(this, (v, year, month, dayOfMonth) -> {
                                task.setDueDateTime(CalendarUtils.getTimeInMillis(year, month, dayOfMonth));
                                changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
                            }, System.currentTimeMillis());
                            break;
                    }
                    return true;
                });
                popupMenu.show();
            });

            View remindDateView = findViewById(R.id.task_details_remind_date);
            ImageView remindDateIcon = findViewById(R.id.task_details_remind_date_icon);
            TextView remindDateText = findViewById(R.id.task_details_remind_date_text);
            View remindDateClearView = findViewById(R.id.task_details_remind_date_clear);
            changeRemindDateFields(remindDateText, remindDateIcon, task);
            remindDateClearView.setOnClickListener(view -> {
                task.setRemindDateTime(null);
                AlarmService.cancelTaskReminder(this, task);
                changeRemindDateFieldsAndSave(remindDateText, remindDateIcon, task);
            });
            remindDateView.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(this, view);
                popupMenu.inflate(R.menu.menu_remind_date);
                popupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_tasks_remind_date_tomorrow:
                            task.setRemindDateTime(CalendarUtils.getTomorrowAtNineDateTimeInMillis());
                            AlarmService.setTaskReminder(this, task);
                            changeRemindDateFieldsAndSave(remindDateText, remindDateIcon, task);
                            break;
                        case R.id.menu_tasks_remind_date_next_week:
                            task.setRemindDateTime(CalendarUtils.getNextMondayAtNineDateTimeInMillis());
                            AlarmService.setTaskReminder(this, task);
                            changeRemindDateFieldsAndSave(remindDateText, remindDateIcon, task);
                            break;
                        case R.id.menu_tasks_select_remind_date:
                            DateTimePickerDialog dateTimePickerDialog = new DateTimePickerDialog(this,
                                    task.getRemindDateTime(), dateTime -> {
                                task.setRemindDateTime(dateTime);
                                AlarmService.setTaskReminder(this, task);
                                changeRemindDateFieldsAndSave(remindDateText, remindDateIcon, task);
                            });
                            dateTimePickerDialog.show();
                            break;
                    }
                    return true;
                });
                popupMenu.show();
            });

            CheckBox checkBox = findViewById(R.id.task_details_main_checkbox);
            checkBox.setChecked(task.getDone());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (task.getDone() != isChecked) {
                    task.setDone(buttonView.isChecked());
                    taskService.update(task);
                }
                if (isChecked) {
                    AlarmService.cancelTaskReminder(this, task);
                } else {
                    AlarmService.setTaskReminder(this, task);
                }
                prepareTaskName(taskNameView, isChecked);
                doneCancelButtonIcon.setImageDrawable(isChecked ? DRAWABLE_CANCEL : DRAWABLE_DONE);
                changeDueDateFieldsAndSave(dueDateText, dueDateIcon, repeatView, task);
                ActivityUtils.vibrate(this);
            });

            View doneCancelButton = findViewById(R.id.task_detail_bottom_bar_done);
            doneCancelButtonIcon = findViewById(R.id.task_detail_bottom_bar_done_icon);
            doneCancelButtonIcon.setImageDrawable(task.getDone() ? DRAWABLE_CANCEL : DRAWABLE_DONE);
            doneCancelButton.setOnClickListener(view -> {
                checkBox.setChecked(!task.getDone());
            });

            View deleteButton = findViewById(R.id.task_detail_bottom_bar_delete);
            deleteButton.setOnClickListener(view -> {
                ActivityUtils.showConfirmationDialog(this, R.string.task_delete_confirmation,
                        (dialog, which) -> {
                            taskService.delete(task);
                            onBackPressed();
                        });
            });
        }
    }

    private Task buildTask(Bundle extras) {
        Task task = new Task();
        task.setId((Integer) extras.get(Task.Fields.ID.fieldName()));
        task.setName(extras.getString(Task.Fields.NAME.fieldName(), StringUtils.EMPTY));
        task.setDone(extras.getBoolean(Task.Fields.DONE.fieldName(), false));
        task.setDueDateTime((Long) extras.get(Task.Fields.DUE_DATE_TIME.fieldName()));
        task.setRemindDateTime((Long) extras.get(Task.Fields.REMIND_DATE_TIME.fieldName()));
        task.setCreatedDateTime((Long) extras.get(Task.Fields.CREATED_DATE_TIME.fieldName()));
        task.setPosition((Integer) extras.get(Task.Fields.POSITION.fieldName()));
        return task;
    }

    private void crossOutText(EditText view, boolean checked) {
        view.setPaintFlags(checked
                ? view.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : view.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
    }

    private void prepareTaskName(EditText taskNameView, boolean isChecked) {
        crossOutText(taskNameView, isChecked);
        taskNameView.setTextColor(isChecked ? COLOR_BLACK_SECONDARY : COLOR_BLACK_PRIMARY);
    }

    private void changeRemindDateFields(TextView remindDateText, ImageView remindDateIcon, Task task) {
        if (null == task.getRemindDateTime() || System.currentTimeMillis() > task.getRemindDateTime()) {
            remindDateText.setTextColor(COLOR_BLACK_SECONDARY);
            remindDateIcon.setColorFilter(COLOR_BLACK_SECONDARY);
            remindDateText.setText(getString(R.string.task_remind_me));
            task.setRemindDateTime(null);
        } else {
            remindDateText.setTextColor(COLOR_BLUE);
            remindDateIcon.setColorFilter(COLOR_BLUE);
            remindDateText.setText(ActivityUtils.getStringDate(this, task.getRemindDateTime(), true));
        }
    }

    private void changeRemindDateFieldsAndSave(TextView remindDateText, ImageView remindDateIcon, Task task) {
        changeRemindDateFields(remindDateText, remindDateIcon, task);
        taskService.update(task);
    }

    private void changeDueDateFields(TextView dueDateText, ImageView dueDateIcon, View repeatView, Task task) {
        if (null != task.getDueDateTime()) {
            if (System.currentTimeMillis() > task.getDueDateTime() && !task.getDone()) {
                dueDateText.setTextColor(COLOR_RED);
                dueDateIcon.setColorFilter(COLOR_RED);
            } else {
                dueDateText.setTextColor(COLOR_BLUE);
                dueDateIcon.setColorFilter(COLOR_BLUE);
            }
            repeatView.setVisibility(View.VISIBLE);
            dueDateText.setText(ActivityUtils.getStringDate(this, task.getDueDateTime(), false));
        } else {
            dueDateText.setTextColor(COLOR_BLACK_SECONDARY);
            dueDateIcon.setColorFilter(COLOR_BLACK_SECONDARY);
            dueDateText.setText(getString(R.string.task_due_date));
        }
    }

    private void changeDueDateFieldsAndSave(TextView dueDateText, ImageView dueDateIcon, View repeatView, Task task) {
        changeDueDateFields(dueDateText, dueDateIcon, repeatView, task);
        taskService.update(task);
    }

    private void initStaticResources() {
        if (0 == COLOR_BLUE) {
            COLOR_BLUE = ContextCompat.getColor(this, R.color.blue);
        }
        if (0 == COLOR_RED) {
            COLOR_RED = ContextCompat.getColor(this, R.color.red);
        }
        if (0 == COLOR_BLACK_PRIMARY) {
            COLOR_BLACK_PRIMARY = ContextCompat.getColor(this, R.color.black_primary);
        }
        if (0 == COLOR_BLACK_SECONDARY) {
            COLOR_BLACK_SECONDARY = ContextCompat.getColor(this, R.color.black_secondary);
        }
        if (null == DRAWABLE_CANCEL) {
            DRAWABLE_CANCEL = ContextCompat.getDrawable(this, R.drawable.icon_cancel_white_24dp);
        }
        if (null == DRAWABLE_DONE) {
            DRAWABLE_DONE = ContextCompat.getDrawable(this, R.drawable.icon_done_white_24dp);
        }
    }
}
