package com.hose.aureliano.project.done.activity.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hose.aureliano.project.done.R;
import com.hose.aureliano.project.done.activity.TaskDetailsActivity;
import com.hose.aureliano.project.done.activity.adapter.api.Adapter;
import com.hose.aureliano.project.done.activity.dialog.TaskModal;
import com.hose.aureliano.project.done.model.Task;
import com.hose.aureliano.project.done.model.TasksViewEnum;
import com.hose.aureliano.project.done.service.TaskService;
import com.hose.aureliano.project.done.service.schedule.alarm.AlarmService;
import com.hose.aureliano.project.done.utils.ActivityUtils;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Adapter for {@link RecyclerView} of the {@link Task}s.
 * <p>
 * Date: 05.02.2018.
 *
 * @author evere
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> implements Adapter<Task> {

    private static int COLOR_RED_SECONDARY;
    private static int COLOR_BLACK_PRIMARY;
    private static int COLOR_BLACK_SECONDARY;
    private static int COLOR_WHITE;
    private static int COLOR_GRAY_LIGHT;

    private Set<Integer> selectedIdsSet;
    private FragmentManager fragmentManager;
    private TaskService taskService;
    private List<Task> taskList;
    private Context context;
    private Integer listId;
    private TasksViewEnum view;
    private ActionMode actionMode;

    /**
     * Controller.
     *
     * @param context         context
     * @param fragmentManager fragment manager
     * @param listId          identifier of the list
     */
    public TaskAdapter(Context context, FragmentManager fragmentManager, Integer listId,
                       TasksViewEnum view) {
        taskService = new TaskService(context);
        this.fragmentManager = fragmentManager;
        this.context = context;
        this.listId = listId;
        this.view = view;
        selectedIdsSet = new HashSet<>();
        initStaticResources();
        refresh();
    }

    /**
     * @return {@link TasksViewEnum}.
     */
    public TasksViewEnum getTasksView() {
        return view;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_task_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.checkBox.setOnClickListener(v -> ActivityUtils.vibrate(context));
        viewHolder.menu.setOnClickListener(menuView -> {
            Task currentTask = getItem(menuView);
            PopupMenu popupMenu = new PopupMenu(context, menuView);
            popupMenu.inflate(R.menu.menu_list_more);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.menu_delete:
                        ActivityUtils.showConfirmationDialog(context, R.string.task_delete_confirmation,
                                (dialog, which) -> {
                                    taskService.delete(currentTask);
                                    removeItem(getPosition(menuView));
                                });
                        break;
                    case R.id.menu_edit:
                        DialogFragment dialog = new TaskModal();
                        dialog.setArguments(buildBundle(currentTask));
                        dialog.show(fragmentManager, "task_modal");
                        break;
                }
                return true;
            });
            popupMenu.show();
        });

        view.setOnClickListener(itemView -> {
            if (null != actionMode) {
                toggleSelection(viewHolder, actionMode);
                ActivityUtils.vibrate(context);
            } else {
                Intent intent = new Intent(context, TaskDetailsActivity.class);
                Task task = getItem(viewHolder.getAdapterPosition());
                intent.putExtra("listId", listId);
                intent.putExtra("taskName", task.getName());
                intent.putExtra("taskId", task.getId());
                intent.putExtra("done", task.getDone());
                context.startActivity(intent,
                        ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context).toBundle());
            }
        });

        view.setOnLongClickListener(longClickView -> {
            if (null != actionMode) {
                if (1 == CollectionUtils.size(selectedIdsSet) && selectedIdsSet.contains(viewHolder.getAdapterPosition())) {
                    return false;
                } else {
                    toggleSelection(viewHolder, actionMode);
                }
            }
            return true;
        });
        view.setTag(viewHolder);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = getItem(position);
        holder.checkBox.setTag(task.getId());
        holder.menu.setTag(task.getId());
        holder.name.setText(task.getName());
        holder.name.setTextColor(task.getDone() ? COLOR_BLACK_SECONDARY : COLOR_BLACK_PRIMARY);
        crossOutText(holder, task.getDone());
        holder.checkBox.setChecked(task.getDone());

        if (null != task.getDueDateTime()) {
            holder.dueDateText
                    .setText(ActivityUtils.getStringDate(context, task.getDueDateTime(), task.getDueTimeIsSet()));
        }
        if (null != task.getRemindDateTime()) {
            holder.reminderText
                    .setText(ActivityUtils.getStringDate(context, task.getRemindDateTime(), task.getRemindTimeIsSet()));
        }

        if (null != task.getDueDateTime() || null != task.getRemindDateTime() && !task.getDone()) {
            setVisibility(holder.taskInfoLayout, true);
            setVisibility(holder.dueDateIcon, null != task.getDueDateTime());
            setVisibility(holder.dueDateText, null != task.getDueDateTime());
            if (!task.getDone() && null != task.getRemindDateTime()
                    && System.currentTimeMillis() < task.getRemindDateTime()) {
                setVisibility(holder.dueDateAndReminderDelimiter, null != task.getDueDateTime());
                setVisibility(holder.reminderIcon, true);
                setVisibility(holder.reminderText, null == task.getDueDateTime());
            } else {
                setVisibility(holder.dueDateAndReminderDelimiter, false);
                setVisibility(holder.reminderIcon, false);
                setVisibility(holder.reminderText, false);
            }
            colorDueDate(task, holder, task.getDone());
        } else {
            setVisibility(holder.taskInfoLayout, false);
        }

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            crossOutText(holder, isChecked);
            Task currentTask = getItem(buttonView);
            if (currentTask.getDone() != buttonView.isChecked()) {
                currentTask.setDone(buttonView.isChecked());
                taskService.update(currentTask);
            }
            if (isChecked && currentTask.getRemindTimeIsSet()) {
                setVisibility(holder.reminderIcon, false);
                setVisibility(holder.reminderText, false);
                setVisibility(holder.dueDateAndReminderDelimiter, false);
                AlarmService.cancelTaskReminder(context, currentTask);
            } else if (!isChecked && currentTask.getRemindTimeIsSet()) {
                if (currentTask.getRemindDateTime() > System.currentTimeMillis()) {
                    AlarmService.setTaskReminder(context, currentTask);
                    setVisibility(holder.taskInfoLayout, true);
                    setVisibility(holder.reminderIcon, true);
                    setVisibility(holder.reminderText, null == currentTask.getDueDateTime());
                    setVisibility(holder.dueDateAndReminderDelimiter, null != currentTask.getDueDateTime());
                } else {
                    taskService.deleteReminderDate(currentTask.getId());
                    currentTask.setRemindDateTime(null);
                    currentTask.setRemindTimeIsSet(false);
                    setVisibility(holder.reminderIcon, false);
                    setVisibility(holder.reminderText, false);
                    setVisibility(holder.dueDateAndReminderDelimiter, false);
                }
            }
            colorDueDate(currentTask, holder, isChecked);
            holder.name.setTextColor(isChecked ? COLOR_BLACK_SECONDARY : COLOR_BLACK_PRIMARY);
        });
        holder.viewForeground.setBackgroundColor(
                selectedIdsSet.contains(holder.getAdapterPosition()) ? COLOR_GRAY_LIGHT : COLOR_WHITE);
    }

    /**
     * Sets action mode.
     *
     * @param actionMode instance of {@link ActionMode}
     */
    public void setActionMode(ActionMode actionMode) {
        this.actionMode = actionMode;
    }

    /**
     * @return set of selected {@link Task}s.
     */
    public List<Task> getSelectedTasks() {
        List<Task> selectedTasks = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(selectedIdsSet)) {
            for (Integer position : selectedIdsSet) {
                selectedTasks.add(taskList.get(position));
            }
        }
        return selectedTasks;
    }

    /**
     * Clear set of selected items.
     */
    public void clearSelection() {
        setActionMode(null);
        selectedIdsSet.clear();
        notifyDataSetChanged();
    }

    /**
     * Selects all items.
     */
    public void selectAll() {
        for (int i = 0; i < CollectionUtils.size(taskList); i++) {
            selectedIdsSet.add(i);
        }
        notifyDataSetChanged();
        if (null != actionMode) {
            actionMode.setTitle(context.getString(R.string.task_selected, CollectionUtils.size(selectedIdsSet)));
        }
    }

    public void toggleSelection(TaskAdapter.ViewHolder viewHolder, ActionMode actionMode) {
        if (selectedIdsSet.contains(viewHolder.getAdapterPosition())) {
            selectedIdsSet.remove(viewHolder.getAdapterPosition());
            viewHolder.viewForeground.setBackgroundColor(COLOR_WHITE);
            if (CollectionUtils.isEmpty(selectedIdsSet)) {
                actionMode.finish();
            }
        } else {
            selectedIdsSet.add(viewHolder.getAdapterPosition());
            viewHolder.viewForeground.setBackgroundColor(COLOR_GRAY_LIGHT);
        }
        actionMode.setTitle(context.getString(R.string.task_selected, CollectionUtils.size(selectedIdsSet)));
    }

    @Override
    public int getItemCount() {
        return CollectionUtils.size(taskList);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * @return available position for new task.
     */
    public int getAvailablePosition() {
        int position = 0;
        for (Task task : getItems()) {
            if (task.getPosition() > position) {
                position = task.getPosition();
            }
        }
        return ++position;
    }

    /**
     * Sets position for tasks as their index.
     */
    public void updatePositions() {
        int position = 0;
        for (Task task : getItems()) {
            task.setPosition(position++);
        }
    }

    @Override
    public List<Task> getItems() {
        return taskList;
    }

    /**
     * Refreshes data on UI.
     */
    public void refresh() {
        if (null != listId) {
            taskList = taskService.getTasks(listId);
        } else if (null != view) {
            taskList = taskService.getTasksForView(view);
        }
        notifyDataSetChanged();
    }

    /**
     * Remove {@link Task} from the list.
     *
     * @param position position of the {@link Task} to remove
     */
    public void removeItem(int position) {
        taskList.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public void removeItems(List<Task> tasks) {
        for (Task task : tasks) {
            taskList.remove(task);
        }
        notifyDataSetChanged();
    }

    /**
     * Restore task information the list.
     *
     * @param position position of the {@link Task} to restore
     * @param task     instance of {@link Task} to restore
     */
    public void restoreItem(int position, Task task) {
        taskList.add(position, task);
        notifyItemInserted(position);
    }

    /**
     * Retrieves {@link Task} information specified position.
     *
     * @param position position of the {@link Task} to retrieve
     * @return instance of {@link Task} information specified position
     */
    public Task getItem(int position) {
        return taskList.get(position);
    }

    private Task getItem(View view) {
        return getItemById((int) view.getTag());
    }

    private Task getItemById(int taskId) {
        for (Task task : taskList) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        throw new NoSuchElementException();
    }

    private int getPosition(View view) {
        int position = 0;
        for (Task task : taskList) {
            if (task.getId().equals(view.getTag())) {
                return position;
            }
            position++;
        }
        throw new NoSuchElementException();
    }

    private void setVisibility(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void colorDueDate(Task task, ViewHolder holder, boolean isChecked) {
        if (null != task.getDueDateTime()) {
            long currentTime = System.currentTimeMillis();
            holder.dueDateIcon.setColorFilter(currentTime > task.getDueDateTime() && !isChecked
                    ? COLOR_RED_SECONDARY : COLOR_BLACK_SECONDARY);
            holder.dueDateText.setTextColor(currentTime > task.getDueDateTime() && !isChecked
                    ? COLOR_RED_SECONDARY : COLOR_BLACK_SECONDARY);
        }
    }

    private void crossOutText(ViewHolder holder, boolean checked) {
        holder.name.setPaintFlags(checked
                ? holder.name.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : holder.name.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
    }

    private Bundle buildBundle(Task task) {
        Bundle bundle = new Bundle();
        bundle.putInt(Task.Fields.ID.getFieldName(), task.getId());
        bundle.putString(Task.Fields.NAME.getFieldName(), task.getName());
        bundle.putBoolean(Task.Fields.DONE.getFieldName(), task.getDone());
        if (null != task.getPosition()) {
            bundle.putInt(Task.Fields.POSITION.getFieldName(), task.getPosition());
        }
        if (null != task.getDueDateTime()) {
            bundle.putLong(Task.Fields.DUE_DATE_TIME.getFieldName(), task.getDueDateTime());
            bundle.putBoolean(Task.Fields.DUE_TIME_IS_SET.getFieldName(), task.getDueTimeIsSet());
        }
        if (null != task.getRemindDateTime()) {
            bundle.putLong(Task.Fields.REMIND_DATE_TIME.getFieldName(), task.getRemindDateTime());
            bundle.putBoolean(Task.Fields.REMIND_TIME_IS_SET.getFieldName(), task.getRemindTimeIsSet());
        }
        if (null != task.getCreatedDateTime()) {
            bundle.putLong(Task.Fields.CREATED_DATE_TIME.getFieldName(), task.getCreatedDateTime());
        }
        return bundle;
    }

    private void initStaticResources() {
        if (0 == COLOR_RED_SECONDARY) {
            COLOR_RED_SECONDARY = ContextCompat.getColor(context, R.color.red);
        }
        if (0 == COLOR_BLACK_PRIMARY) {
            COLOR_BLACK_PRIMARY = ContextCompat.getColor(context, R.color.black_primary);
        }
        if (0 == COLOR_BLACK_SECONDARY) {
            COLOR_BLACK_SECONDARY = ContextCompat.getColor(context, R.color.black_secondary);
        }
        if (0 == COLOR_WHITE) {
            COLOR_WHITE = ContextCompat.getColor(context, R.color.white);
        }
        if (0 == COLOR_GRAY_LIGHT) {
            COLOR_GRAY_LIGHT = ContextCompat.getColor(context, R.color.lightGray);
        }
    }

    /**
     * Provides a reference to the views for each task item.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private FrameLayout view;
        private RelativeLayout viewForeground;
        private RelativeLayout viewBackground;
        private TextView backgroundRightText;
        private TextView backgroundLeftText;
        private ImageView backgroundRightIcon;
        private ImageView backgroundLeftIcon;
        private TextView name;
        private View menu;
        private CheckBox checkBox;
        private RelativeLayout taskInfoLayout;
        private ImageView dueDateIcon;
        private TextView dueDateText;
        private TextView dueDateAndReminderDelimiter;
        private ImageView reminderIcon;
        private TextView reminderText;

        /**
         * Controller.
         *
         * @param view task view
         */
        ViewHolder(View view) {
            super(view);
            this.taskInfoLayout = view.findViewById(R.id.task_info_layout);
            this.dueDateIcon = view.findViewById(R.id.task_info_due_icon);
            this.dueDateText = view.findViewById(R.id.task_info_due_text);
            this.dueDateAndReminderDelimiter = view.findViewById(R.id.task_info_delimiter_before_reminder);
            this.reminderIcon = view.findViewById(R.id.task_info_reminder_icon);
            this.reminderText = view.findViewById(R.id.task_info_reminder_text);
            this.name = view.findViewById(R.id.task_name);
            this.menu = view.findViewById(R.id.task_menu);
            this.checkBox = view.findViewById(R.id.task_checkbox);
            this.view = view.findViewById(R.id.task_item_layout);
            this.viewForeground = view.findViewById(R.id.item_view_foreground);
            this.viewBackground = view.findViewById(R.id.item_view_background);
            this.backgroundRightIcon = view.findViewById(R.id.task_background_delete_icon);
            this.backgroundLeftIcon = view.findViewById(R.id.task_background_done_icon);
            this.backgroundRightText = view.findViewById(R.id.task_background_delete_text);
            this.backgroundLeftText = view.findViewById(R.id.task_background_done_text);

            this.dueDateIcon.setVisibility(View.GONE);
            this.dueDateText.setVisibility(View.GONE);
        }

        public FrameLayout getView() {
            return view;
        }

        public RelativeLayout getViewForeground() {
            return viewForeground;
        }

        public RelativeLayout getViewBackground() {
            return viewBackground;
        }

        public TextView getBackgroundRightText() {
            return backgroundRightText;
        }

        public TextView getBackgroundLeftText() {
            return backgroundLeftText;
        }

        public ImageView getBackgroundRightIcon() {
            return backgroundRightIcon;
        }

        public ImageView getBackgroundLeftIcon() {
            return backgroundLeftIcon;
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }
    }
}
