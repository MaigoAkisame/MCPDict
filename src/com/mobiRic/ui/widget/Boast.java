// Cancel the last toast (if it still there) before displaying the next
// Reference: http://stackoverflow.com/a/16103514

package com.mobiRic.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.widget.Toast;

/**
 * {@link Toast} decorator allowing for easy cancellation of notifications. Use
 * this class if you want subsequent Toast notifications to overwrite current
 * ones. </p>
 *
 * By default, a current {@link Boast} notification will be cancelled by a
 * subsequent notification. This default behaviour can be changed by calling
 * certain methods like {@link #show(boolean)}.
 */
public class Boast
{
    /**
     * Keeps track of certain {@link Boast} notifications that may need to be cancelled.
     * This functionality is only offered by some of the methods in this class.
     */
    private volatile static Boast globalBoast = null;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal reference to the {@link Toast} object that will be displayed.
     */
    private Toast internalToast;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Private constructor creates a new {@link Boast} from a given
     * {@link Toast}.
     *
     * @throws NullPointerException
     *         if the parameter is <code>null</code>.
     */
    private Boast(Toast toast)
    {
        // null check
        if (toast == null)
        {
            throw new NullPointerException(
                "Boast.Boast(Toast) requires a non-null parameter.");
        }

        internalToast = toast;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Make a standard {@link Boast} that just contains a text view.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param text
     *        The text to show. Can be formatted text.
     * @param duration
     *        How long to display the message. Either {@link #LENGTH_SHORT} or
     *        {@link #LENGTH_LONG}
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text,
        int duration)
    {
        return new Boast(Toast.makeText(context, text, duration));
    }

    /**
     * Make a standard {@link Boast} that just contains a text view with the
     * text from a resource.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param resId
     *        The resource id of the string resource to use. Can be formatted
     *        text.
     * @param duration
     *        How long to display the message. Either {@link #LENGTH_SHORT} or
     *        {@link #LENGTH_LONG}
     *
     * @throws Resources.NotFoundException
     *         if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId, int duration)
        throws Resources.NotFoundException
    {
        return new Boast(Toast.makeText(context, resId, duration));
    }

    /**
     * Make a standard {@link Boast} that just contains a text view. Duration
     * defaults to {@link #LENGTH_SHORT}.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param text
     *        The text to show. Can be formatted text.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, CharSequence text)
    {
        return new Boast(Toast.makeText(context, text, Toast.LENGTH_SHORT));
    }

    /**
     * Make a standard {@link Boast} that just contains a text view with the
     * text from a resource. Duration defaults to {@link #LENGTH_SHORT}.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param resId
     *        The resource id of the string resource to use. Can be formatted
     *        text.
     *
     * @throws Resources.NotFoundException
     *         if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    public static Boast makeText(Context context, int resId)
        throws Resources.NotFoundException
    {
        return new Boast(Toast.makeText(context, resId, Toast.LENGTH_SHORT));
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Show a standard {@link Boast} that just contains a text view.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param text
     *        The text to show. Can be formatted text.
     * @param duration
     *        How long to display the message. Either {@link #LENGTH_SHORT} or
     *        {@link #LENGTH_LONG}
     */
    public static void showText(Context context, CharSequence text, int duration)
    {
        Boast.makeText(context, text, duration).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view with the
     * text from a resource.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param resId
     *        The resource id of the string resource to use. Can be formatted
     *        text.
     * @param duration
     *        How long to display the message. Either {@link #LENGTH_SHORT} or
     *        {@link #LENGTH_LONG}
     *
     * @throws Resources.NotFoundException
     *         if the resource can't be found.
     */
    public static void showText(Context context, int resId, int duration)
        throws Resources.NotFoundException
    {
        Boast.makeText(context, resId, duration).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view. Duration
     * defaults to {@link #LENGTH_SHORT}.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param text
     *        The text to show. Can be formatted text.
     */
    public static void showText(Context context, CharSequence text)
    {
        Boast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a standard {@link Boast} that just contains a text view with the
     * text from a resource. Duration defaults to {@link #LENGTH_SHORT}.
     *
     * @param context
     *        The context to use. Usually your {@link android.app.Application}
     *        or {@link android.app.Activity} object.
     * @param resId
     *        The resource id of the string resource to use. Can be formatted
     *        text.
     *
     * @throws Resources.NotFoundException
     *         if the resource can't be found.
     */
    public static void showText(Context context, int resId)
        throws Resources.NotFoundException
    {
        Boast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet.
     * You do not normally have to call this. Normally view will disappear on
     * its own after the appropriate duration.
     */
    public void cancel()
    {
        internalToast.cancel();
    }

    /**
     * Show the view for the specified duration. By default, this method cancels
     * any current notification to immediately display the new one. For
     * conventional {@link Toast#show()} queueing behaviour, use method
     * {@link #show(boolean)}.
     *
     * @see #show(boolean)
     */
    public void show()
    {
        show(true);
    }

    /**
     * Show the view for the specified duration. This method can be used to
     * cancel the current notification, or to queue up notifications.
     *
     * @param cancelCurrent
     *        <code>true</code> to cancel any current notification and replace
     *        it with this new one
     *
     * @see #show()
     */
    public void show(boolean cancelCurrent)
    {
        // cancel current
        if (cancelCurrent && (globalBoast != null))
        {
            globalBoast.cancel();
        }

        // save an instance of this current notification
        globalBoast = this;

        internalToast.show();
    }

}
