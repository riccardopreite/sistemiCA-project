package it.unibo.socialplaces.fragment.dialog.handler

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import it.unibo.socialplaces.domain.Friends
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalStateException

class FriendRequestDialogFragment: DialogFragment() {
    private var friendUsername: String = ""
    private var isRequestAccepted: Boolean = false
    companion object {
        private const val FRIENDUSERNAME = "friendUsername"

        @JvmStatic
        fun newInstance(friendUsername:String) =
            FriendRequestDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(FRIENDUSERNAME, friendUsername)
                }
            }

        private val TAG: String = FriendRequestDialogFragment::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            friendUsername = it.getString(FRIENDUSERNAME)!!
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle("New frienship request!")
            builder.setMessage("$friendUsername sent you a frienship request.")

            builder.setPositiveButton("Accept") { dialog, _ ->
                isRequestAccepted = true
                dialog.dismiss()
            }
            builder.setNegativeButton("Deny") { dialog, _ ->
                isRequestAccepted = false
                dialog.dismiss()
            }
            isCancelable = false
            val dialog = builder.create()


            dialog.setCanceledOnTouchOutside(false)
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        CoroutineScope(Dispatchers.IO).launch {
            if (isRequestAccepted) {
                Friends.confirmFriend(friendUsername)
                Log.i(TAG, "Friend request accepted!")
            } else {
                Friends.denyFriend(friendUsername)
                Log.i(TAG, "Friend request denied!")
            }
        }

    }


}