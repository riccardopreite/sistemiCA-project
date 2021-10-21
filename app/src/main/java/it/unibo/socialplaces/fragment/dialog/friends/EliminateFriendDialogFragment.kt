package it.unibo.socialplaces.fragment.dialog.friends

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.lang.ClassCastException
import java.lang.IllegalStateException

class EliminateFriendDialogFragment: DialogFragment() {
    // Listener
    interface EliminateFriendDialogListener {
        fun onDeleteButtonPressed(dialog: DialogFragment, friendUsername: String)
        fun onCancelDeletionButtonPressed(dialog: DialogFragment)
        fun onDeletionConfirmation(dialog: DialogFragment)
    }

    internal lateinit var listener: EliminateFriendDialogListener

    // App state
    private var friendUsername: String? = null

    companion object {
        private val TAG: String = EliminateFriendDialogFragment::class.qualifiedName!!

        private const val ARG_FRIENDUSERNAME = "friendUsername"

        @JvmStatic
        fun newInstance(friendUsername: String) =
            EliminateFriendDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FRIENDUSERNAME, friendUsername)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendUsername = it.getString(ARG_FRIENDUSERNAME)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_eliminate_item, null)
            val deleteBtn = dialogView.findViewById<Button>(R.id.delete_item)

            deleteBtn.setOnClickListener { v ->
                friendUsername?.let { friend ->
                    listener.onDeleteButtonPressed(this, friend)

                    val snackbar = Snackbar.make(
                        it.findViewById(android.R.id.content),
                        R.string.removed_friend,
                        5000
                    ).setAction(R.string.cancel) {
                        listener.onCancelDeletionButtonPressed(this@EliminateFriendDialogFragment)
                        Toast.makeText(it.context, R.string.canceled_removal, Toast.LENGTH_LONG)
                            .show()
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