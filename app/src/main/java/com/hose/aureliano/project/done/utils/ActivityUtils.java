package com.hose.aureliano.project.done.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.text.format.DateUtils;
import android.view.View;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

/**
 * Header.
 * <p>
 * Date: 2/13/2018.
 *
 * @author Uladzislau_Shalamits
 */
public final class ActivityUtils {

    private ActivityUtils() {
        throw new AssertionError();
    }

    /**
     * Show a {@link Snackbar}.
     *
     * @param view view
     * @param text text to show on {@link Snackbar}
     * @return instance of {@link Snackbar}
     */
    public static Snackbar showSnackBar(View view, String text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
        snackbar.show();
        return snackbar;
    }

    /**
     * Show a {@link Snackbar}.
     *
     * @param view            view
     * @param text            text to show on {@link Snackbar}
     * @param actionTextResId resource id for action text
     * @param listener        instance of {@link View.OnClickListener}
     * @return instance of {@link Snackbar}
     */
    public static Snackbar showSnackBar(View view, String text, int actionTextResId,
                                        View.OnClickListener listener) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionTextResId, listener);
        snackbar.show();
        return snackbar;
    }

    /**
     * Vibrate constantly for the specified period of time.
     *
     * @param context      context
     * @param milliseconds number of milliseconds to vibrate
     */
    public static void vibrate(Context context, long milliseconds) {
        Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
        if (null != vibrator) {
            vibrator.vibrate(milliseconds);
        }
    }

    /**
     * Shows {@link DatePickerDialog} with current date on UI.
     *
     * @param context  context
     * @param listener instance of {@link DatePickerDialog.OnDateSetListener}
     * @return {@link DatePickerDialog} on UI
     */
    public static DatePickerDialog showDatePickerDialog(Context context, DatePickerDialog.OnDateSetListener listener,
                                                        Long timeInMilliseconds) {
        GregorianCalendar calendar = new GregorianCalendar();
        if (timeInMilliseconds != null) {
            calendar.setTimeInMillis(timeInMilliseconds);
        }
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, listener, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
        return datePickerDialog;
    }

    /**
     * Convert instance of {@link Calendar} into String.
     *
     * @param context      context
     * @param dateInMillis date in milliseconds
     * @return converted date
     */
    public static String getStringDate(Context context, Long dateInMillis) {
        Objects.requireNonNull(dateInMillis);
        return DateUtils.formatDateTime(context, dateInMillis, DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_ALL);
    }
}
