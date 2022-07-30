package com.group2.concord_messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import com.google.android.material.textfield.TextInputLayout

fun showFieldError(view: TextInputLayout, message: String)
{
    view.error = message
}

fun clearFieldErrors(views: List<TextInputLayout>)
{
    for(view in views)
    {
        view.error = null
    }
}

fun clearFocuses(views: List<View>)
{
    for(view in views)
    {
        view.clearFocus()
    }
}

fun closeKeyboard(activity: Activity)
{
    val focus = activity.currentFocus
    if(focus != null)
    {
        val inputMethodManager =
            activity.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(focus.windowToken, 0)
    }
}

fun setLoading(activity: Activity, progressBarView: ProgressBar, views: List<View>)
{
    activity.window.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    progressBarView.visibility = View.VISIBLE
    for(view in views)
    {
        view.isEnabled = false
    }
}

fun clearLoading(activity: Activity, progressBarView: ProgressBar, views: List<View>)
{
    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    progressBarView.visibility = View.INVISIBLE
    for(view in views)
    {
        view.isEnabled = true
    }
}

fun startActivityClear(context: Context, className: Class<*>)
{
    val intent = Intent(context, className)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}