package com.example.maptry.fragment.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.lang.ClassCastException
import java.lang.IllegalStateException

class EliminateFriendDialogFragment: DialogFragment() {
    interface EliminateFriendDialogListener {
        fun onDeleteButtonPressed(dialog: DialogFragment)
        fun onCancelDeletionButtonPressed(dialog: DialogFragment)
        fun onDeletionConfirmation(dialog: DialogFragment)
    }

    internal lateinit var listener: EliminateFriendDialogListener

    companion object {
        private val TAG: String = EliminateFriendDialogFragment::class.qualifiedName!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val deleteBtn = dialogView.findViewById<Button>(R.id.eliminateBtn)

            deleteBtn.setOnClickListener { view ->
                listener.onDeleteButtonPressed(this)

                val snackbar = Snackbar.make(view, R.string.removed_friend, 5000).setAction(R.string.cancel) {
                    listener.onCancelDeletionButtonPressed(this@EliminateFriendDialogFragment)
                    Toast.makeText(view.context, R.string.canceled_removal, Toast.LENGTH_LONG).show()
                }
                snackbar.setActionTextColor(Color.DKGRAY)
                snackbar.view.setBackgroundColor(Color.BLACK)

                snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        listener.onDeletionConfirmation(this@EliminateFriendDialogFragment)
                    }
                })

                snackbar.show()
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as EliminateFriendDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement EliminateFriendDialogListener")
        }
    }
}