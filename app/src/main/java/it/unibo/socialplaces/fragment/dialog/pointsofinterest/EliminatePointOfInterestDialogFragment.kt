package it.unibo.socialplaces.fragment.dialog.pointsofinterest

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

class EliminatePointOfInterestDialogFragment: DialogFragment() {
    // Listener
    interface EliminatePointOfInterestDialogListener {
        fun onDeleteButtonPressed(dialog: DialogFragment, poiName: String)
        fun onCancelDeletionButtonPressed(dialog: DialogFragment)
        fun onDeletionConfirmation(dialog: DialogFragment)
    }

    internal lateinit var listener: EliminatePointOfInterestDialogListener

    // App state
    private var poiName: String? = null

    companion object {
        private val TAG: String = EliminatePointOfInterestDialogFragment::class.qualifiedName!!

        private const val ARG_POINAME = "poiName"

        @JvmStatic
        fun newInstance(poiName: String) =
            EliminatePointOfInterestDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POINAME, poiName)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            poiName = it.getString(ARG_POINAME)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.v(TAG, "onCreateDialog")
        return activity?.let { it ->
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_eliminate_item, null)
            val deleteBtn = dialogView.findViewById<Button>(R.id.delete_item)

            deleteBtn.setOnClickListener { _ ->
                poiName?.let { name ->
                    listener.onDeleteButtonPressed(this, name)

                    val snackbar = Snackbar.make(
                        it.findViewById(android.R.id.content),
                        R.string.removed_poi,
                        5000
                    ).setAction(R.string.cancel) {
                        listener.onCancelDeletionButtonPressed(this@EliminatePointOfInterestDialogFragment)
                        Toast.makeText(it.context, R.string.canceled_removal, Toast.LENGTH_LONG).show()
                    }.apply {
                        setActionTextColor(Color.DKGRAY)
                        view.setBackgroundColor(Color.BLACK)

                        addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                listener.onDeletionConfirmation(this@EliminatePointOfInterestDialogFragment)
                            }
                        })
                    }

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
            listener = context as EliminatePointOfInterestDialogListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement EliminatePointOfInterestDialogListener")
        }
    }
}