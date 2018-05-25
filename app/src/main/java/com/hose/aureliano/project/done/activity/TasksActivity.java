package com.hose.aureliano.project.done.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.hose.aureliano.project.done.R;
import com.hose.aureliano.project.done.activity.adapter.TaskAdapter;
import com.hose.aureliano.project.done.activity.component.CustomEditText;
import com.hose.aureliano.project.done.activity.component.RecyclerViewEmptySupport;
import com.hose.aureliano.project.done.activity.dialog.TaskModal;
import com.hose.aureliano.project.done.activity.helper.TaskItemTouchHelper;
import com.hose.aureliano.project.done.model.Task;
import com.hose.aureliano.project.done.model.TasksViewEnum;
import com.hose.aureliano.project.done.service.TaskService;
import com.hose.aureliano.project.done.service.schedule.alarm.AlarmService;
import com.hose.aureliano.project.done.utils.ActivityUtils;
import com.hose.aureliano.project.done.utils.AnimationUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Date;

/**
 * Activity fot displaying all TODOs for selected list.
 * <p>
 * Date: 12.02.2018.
 *
 * @author Uladzislau Shalamitski
 */
public class TasksActivity extends AppCompatActivity implements TaskModal.TaskDialogListener {

    private String listId;
    private View coordinator;
    private TaskAdapter taskAdapter;
    private FloatingActionButton floatingActionButton;
    private TaskService taskService = new TaskService(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        Toolbar toolbar = findViewById(R.id.tasks_toolbar);
        toolbar.setNavigationIcon(R.drawable.icon_arrow_back_white_24);
        setSupportActionBar(toolbar);

        TasksViewEnum viewEnum = null != getIntent().getExtras().get("view")
                ? (TasksViewEnum) getIntent().getExtras().get("view") : null;
        if (null != viewEnum) {
            setTitle(getString(R.string.view));
            toolbar.setSubtitle(getIntent().getStringExtra("title"));
        } else {
            setTitle(getIntent().getStringExtra("title"));
        }

        listId = getIntent().getStringExtra("listId");
        coordinator = findViewById(R.id.tasks_coordinator_layout);
        taskAdapter = new TaskAdapter(this, getSupportFragmentManager(), listId, viewEnum);

        RecyclerViewEmptySupport listView = findViewById(R.id.activity_tasks_list_view);
        listView.setEmptyView(findViewById(R.id.activity_tasks_empty_view));
        listView.setAdapter(taskAdapter);
        listView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        floatingActionButton = findViewById(R.id.activity_tasks_fab);

        View bottomView = findViewById(R.id.activity_task_new);
        bottomView.setVisibility(View.GONE);
        bottomView.setOnClickListener(view -> {
        });

        if (TasksViewEnum.OVERDUE != viewEnum) {
            InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            CustomEditText editText = findViewById(R.id.activity_task_new_edit_text);

            ViewGroup decorView = getWindow().getDecorView().findViewById(R.id.activity_task_to_decor);
            ImageView newIcon = findViewById(R.id.activity_task_new_icon);
            View background = LayoutInflater.from(this).inflate(R.layout.decor_view_layout, null);

            background.setOnClickListener(view -> {
                inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
                editText.onKeyPreIme(1, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
            });

            editText.setListener(() -> {
                bottomView.setVisibility(View.GONE);
                AnimationUtil.animateAlphaFadeOut(background, () -> {
                    decorView.removeView(background);
                    floatingActionButton.show();
                });
            });

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (StringUtils.isNoneBlank(s)) {
                        newIcon.setColorFilter(getResources().getColor(R.color.green));
                    } else if (StringUtils.isBlank(s)) {
                        newIcon.setColorFilter(getResources().getColor(R.color.darker_gray));
                    }
                }
            });

            editText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE && StringUtils.isNotBlank(editText.getText().toString())) {
                    int position = taskAdapter.getAvailablePosition();
                    Task task = new Task();
                    task.setListId(listId);
                    task.setName(editText.getText().toString());
                    task.setPosition(position);
                    task.setCreatedDateTime(new Date().getTime());
                    long id = taskService.insert(task);
                    ActivityUtils.handleDbInteractionResult(id, coordinator, () -> {
                        task.setId((int) id);
                        taskAdapter.getItems().add(task);
                        int adapterPos = taskAdapter.getItems().indexOf(task);
                        taskAdapter.notifyItemInserted(adapterPos);
                        listView.scrollToPosition(adapterPos);
                        editText.setText(StringUtils.EMPTY);
                    });
                }
                return true;
            });

            floatingActionButton.setVisibility(View.VISIBLE);
            floatingActionButton.setOnClickListener(view -> {
                bottomView.setVisibility(View.VISIBLE);
                editText.requestFocus();
                if (null != inputManager) {
                    floatingActionButton.hide();
                    inputManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    AnimationUtil.animateAlphaFadeIn(background);
                    decorView.addView(background);
                }
            });
        } else {
            floatingActionButton.setVisibility(View.GONE);
        }

        new ItemTouchHelper(new TaskItemTouchHelper(this, taskAdapter, coordinator)).attachToRecyclerView(listView);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        if (null != floatingActionButton) {
            floatingActionButton.hide();
        }
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        if (null != floatingActionButton) {
            floatingActionButton.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tasks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_tasks_sort_name:
                Collections.sort(taskAdapter.getItems(),
                        (task1, task2) -> StringUtils.compare(task1.getName(), task2.getName()));
                applySortChanges();
                return true;
            case R.id.menu_tasks_sort_due_date:
                Collections.sort(taskAdapter.getItems(), (task1, task2) -> {
                    if (null == task1.getDueDateTime() && null == task2.getDueDateTime()) {
                        return 0;
                    } else if (null == task1.getDueDateTime() && null != task2.getDueDateTime()) {
                        return 1;
                    } else if (null != task1.getDueDateTime() && null == task2.getDueDateTime()) {
                        return -1;
                    } else {
                        return task1.getDueDateTime().compareTo(task2.getDueDateTime());
                    }
                });
                applySortChanges();
                return true;
            case R.id.menu_tasks_sort_create_date:
                Collections.sort(taskAdapter.getItems(), (task1, task2) -> {
                    if (null == task1.getCreatedDateTime() && null == task2.getCreatedDateTime()) {
                        return 0;
                    } else if (null == task1.getCreatedDateTime() && null != task2.getCreatedDateTime()) {
                        return 1;
                    } else if (null != task1.getCreatedDateTime() && null == task2.getCreatedDateTime()) {
                        return -1;
                    } else {
                        return task1.getCreatedDateTime().compareTo(task2.getCreatedDateTime());
                    }
                });
                applySortChanges();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment fragment, Task task) {
        long result;
        TextView name = fragment.getDialog().findViewById(R.id.tasks_modal_name);
        task.setName(name.getText().toString());
        task.setListId(listId);

        result = task.getId() != null ? taskService.update(task) : taskService.insert(task);

        if (result != -1) {
            taskAdapter.refresh();
            ActivityUtils.showSnackBar(coordinator, String.format("done: %s", name.getText()));
            if (task.getRemindTimeIsSet()) {
                AlarmService.setTaskReminder(this, task);
            }
        } else {
            ActivityUtils.showSnackBar(coordinator, "Oops! Something went wrong!");
        }
    }

    private void applySortChanges() {
        taskAdapter.updatePositions();
        taskAdapter.notifyDataSetChanged();
        taskService.update(taskAdapter.getItems());
    }
}
