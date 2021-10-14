package com.example.maptry.fragment.dialog.friends

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.config.Auth
import java.lang.ClassCastException
import java.lang.IllegalStateException

class AddFriendDialogFragment: DialogFragment() {
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
                if(userEmail.split("@")[0] == Auth.getUsername()) {
                    Toast.makeText(context, R.string.no_request_to_oneself, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                listener.sendFriendshipRequest(this, userEmail.split("@")[0])
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