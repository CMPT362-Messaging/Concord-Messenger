package com.group2.concord_messenger.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.group2.concord_messenger.R
import java.lang.IllegalStateException

class AttachPictureDialog : DialogFragment() {

    internal lateinit var listener: ProfilePicturePickDialogListener

    interface ProfilePicturePickDialogListener {
        fun onOpenCameraClick(dialog: DialogFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_profile_picture_way, container, false)
    }

    // taken and modified from example in official android doc
    // https://developer.android.com/guide/topics/ui/dialogs#kotlin
    override fun onAttach(context: Context) {
        super.onAttach(context)

        try {
            listener = context as ProfilePicturePickDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(("$context must implement ProfilePicturePickDialogListener"))
        }
    }

    // fixed problem with dialog not being displayed as match parent
    // https://stackoverflow.com/a/65858167 -- this answer helped
    // as well as official android documentation - https://developer.android.com/guide/topics/ui/dialogs#kotlin
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Do you want to take picture to share?")
                .setItems(R.array.open_camera_options) { dialog, which ->
                    when (which) {
                        0 -> listener.onOpenCameraClick(this)
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


}