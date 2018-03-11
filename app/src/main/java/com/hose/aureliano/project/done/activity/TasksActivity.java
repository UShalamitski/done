package com.hose.aureliano.project.done.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.hose.aureliano.project.done.R;
import com.hose.aureliano.project.done.activity.adapter.TaskAdapter;
import com.hose.aureliano.project.done.activity.dialog.TaskModal;
import com.hose.aureliano.project.done.activity.helper.TaskItemTouchHelper;
import com.hose.aureliano.project.done.model.Task;
import com.hose.aureliano.project.done.repository.DatabaseCreator;
import com.hose.aureliano.project.done.repository.dao.TaskDao;
import com.hose.aureliano.project.done.service.schedule.alarm.AlarmService;
import com.hose.aureliano.project.done.utils.ActivityUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Activity fot displaying all TODOs for selected list.
 * <p>
 * Date: 12.02.2018.
 *
 * @author evere
 */
public class TasksActivity extends AppCompatActivity implements TaskModal.TaskDialogListener {

    private String listId;
    private View coordinator;
    private TaskAdapter taskAdapter;

    private TaskDao taskDao = DatabaseCreator.getDatabase(this).getTaskDao();
    private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(5);
    private Map<Task, ScheduledFuture> itemsToRemoveMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        listId = getIntent().getExtras().get("listId").toString();
        coordinator = findViewById(R.id.tasks_coordinator_layout);
        taskAdapter = new TaskAdapter(this, getSupportFragmentManager(), listId);

        RecyclerView listView = findViewById(R.id.activity_tasks_list_view);
        listView.setAdapter(taskAdapter);
        listView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        FloatingActionButton fab = findViewById(R.id.activity_tasks_fab);
        fab.setOnClickListener(view -> {
            DialogFragment dialogFragment = new TaskModal();
            dialogFragment.show(getSupportFragmentManager(), "tag");
        });

        int swipeDirs = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
        new ItemTouchHelper(new TaskItemTouchHelper(this, taskAdapter, coordinator, swipeDirs, 0))
                .attachToRecyclerView(listView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment fragment, Task task) {
        long result = 0;
        TextView name = fragment.getDialog().findViewById(R.id.tasks_modal_name);
        task.setName(name.getText().toString());
        task.setListId(listId);

        result = task.getId() != null ? taskDao.update(task) : taskDao.insert(task);

        if (result != -1) {
            taskAdapter.refresh();
            ActivityUtils.showSnackBar(coordinator, String.format("done: %s", name.getText()));
            if (task.getRemindTimeIsSet()) {
                AlarmService.setAlarm(this, task);
            }
        } else {
            ActivityUtils.showSnackBar(coordinator, "Oops! Something went wrong!");
        }
    }
}
