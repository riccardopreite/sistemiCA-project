package com.example.maptry.fragment.dialog.pointsofinterest

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
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater
            val dialogView = inflater.inflate(R.layout.dialog_eliminate_item, null)
            val deleteBtn = dialogView.findViewById<Button>(R.id.delete_item)

            deleteBtn.setOnClickListener { v ->
                poiName?.let { name ->
                    listener.onDeleteButtonPressed(this, name)

                    view?.let { fragView ->
                        val snackbar =
                            Snackbar.make(fragView, R.string.removed_poi, 5000).setAction(R.string.cancel) {
                                listener.onCancelDeletionButtonPressed(this@EliminatePointOfInterestDialogFragment)
                                Toast.makeText(
                                    fragView.context,
                                    R.string.canceled_removal,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        snackbar.setActionTextColor(Color.DKGRAY)
                        snackbar.view.setBackgroundColor(Color.BLACK)

                        snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                listener.onDeletionConfirmation(this@EliminatePointOfInterestDialogFragment)
                            }
                        })

                        snackbar.show()
                    }
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