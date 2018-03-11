package com.hose.aureliano.project.done.service.schedule;

import android.content.Context;

import com.hose.aureliano.project.done.model.Task;
import com.hose.aureliano.project.done.repository.DatabaseCreator;
import com.hose.aureliano.project.done.repository.dao.TaskDao;
import com.hose.aureliano.project.done.service.schedule.alarm.AlarmService;

import java.util.List;

/**
 * Service logic for tasks.
 * <p/>
 * Date: 11.03.2018
 *
 * @author Uladzislau Shalamitski
 */
public class TaskService {

    private TaskDao taskDao;
    private Context context;

    public TaskService(Context context) {
        this.context = context;
        taskDao = DatabaseCreator.getDatabase(context).getTaskDao();
    }

    /**
     * Removes all tasks for specified list and reminders for that tasks.
     *
     * @param listId identifier of list
     */
    public void deleteTasks(String listId) {
        List<Task> tasks = taskDao.read(listId);
        for (Task task : tasks) {
            AlarmService.cancelAlarm(context, task);
        }
        taskDao.deleteByListId(listId);
    }

    /**
     * Removes specified {@link Task} and reminder for it.
     *
     * @param task instance of {@link Task} to remove
     */
    public void deleteTask(Task task) {
        AlarmService.cancelAlarm(context, task);
        taskDao.delete(task.getId());
    }
}