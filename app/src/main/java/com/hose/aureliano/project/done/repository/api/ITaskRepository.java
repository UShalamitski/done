package com.hose.aureliano.project.done.repository.api;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by evere on 11.02.2018.
 */

public interface ITaskRepository {

    /**
     * Method for inserting a task into the database.
     *
     * @param values this map contains the initial column values for the row.
     *               The keys should be the column names and the values the column values
     * @return the row ID of the newly inserted row, or -1 if an error occurred
     */
    long insert(ContentValues values);

    /**
     * Deletes all tasks from table.
     *
     * @return the number of affected rows
     */
    int delete();

    /**
     * Deletes task by its identifier.
     *
     * @param taskId task identifier
     * @return the number of affected rows
     */
    int delete(String taskId);

    /**
     * Method for updating a task in the database.
     *
     * @param values this map contains column values for the row.
     *               The keys should be the column names and the values the column values
     * @return the number of affected rows
     */
    int update(ContentValues values);

    /**
     * Retrieves all tasks from the database.
     *
     * @return instance of {@link Cursor} with all tasks
     */
    Cursor read();
}
