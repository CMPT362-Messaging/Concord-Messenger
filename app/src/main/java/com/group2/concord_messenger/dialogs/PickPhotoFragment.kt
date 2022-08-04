package com.group2.concord_messenger.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.group2.concord_messenger.R

// https://developer.android.com/guide/topics/ui/dialogs
class PickPhotoFragment : DialogFragment() {

    var pickedOption: Int? = null;

    internal lateinit var listener: PickPhotoListener

    interface PickPhotoListener {
        fun onComplete(dialog: DialogFragment)
    }


    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as PickPhotoListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement PickPhotoListener"))
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)

            val inflater = requireActivity().layoutInflater;
            builder.setTitle("Pick Profile Picker")
                .setItems(R.array.pic_options, DialogInterface.OnClickListener { dialog, item ->
                    pickedOption = item
                    listener.onComplete(this)
                })
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
