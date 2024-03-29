package it.unibo.socialplaces.fragment.dialog.friends

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.Auth
import java.lang.ClassCastException
import java.lang.IllegalStateException

class AddFriendDialogFragment: DialogFragment() {
    // Listener
    interface AddFriendDialogListener {
        fun sendFriendshipRequest(dialog: DialogFragment, username: String)
    }

    internal lateinit var listener: AddFriendDialogListener

    companion object {
        private val TAG: String = AddFriendDialogFragment::class.qualifiedName!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_add_friend, null)
            val emailEt = dialogView.findViewById<EditText>(R.id.friend_email)
            val sendRequestBtn = dialogView.findViewById<Button>(R.id.send_friend_request)

            sendRequestBtn.setOnClickListener {
                val userEmail = emailEt.text.toString()
                if(userEmail == "" || userEmail == getString(R.string.enter_email)) {
                    Toast.makeText(context, R.string.empty_username, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val friendUsername = userEmail.split("@")[0]
                if(friendUsername == Auth.getUsername()) {
                    Toast.makeText(context, R.string.no_request_to_oneself, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                listener.sendFriendshipRequest(this, friendUsername)
            }

            builder.setView(dialogView)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as AddFriendDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement AddFriendDialogListener")
        }
    }
}