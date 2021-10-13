package com.example.maptry.activity

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.fragment.PointsOfInterestFragment
import com.example.maptry.fragment.dialog.EliminatePointOfInterestDialogFragment
import com.example.maptry.fragment.dialog.PoiDetailsDialogFragment
import com.example.maptry.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PointsOfInterestListActivity: ListActivity(),
    EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener,
    PointsOfInterestFragment.PointsOfInterestDialogListener {
    companion object {
        private val TAG: String = PointsOfInterestListActivity::class.qualifiedName!!

        private const val ARG_POITODELETE = "poiToDelete"
    }

    // App state
    private var poiToDelete: PointOfInterest? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            poiToDelete = it.getParcelable(ARG_POITODELETE) as PointOfInterest?
        }
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest()
            val poisFragment = PointsOfInterestFragment.newInstance(poisList)
            pushFragment(poisFragment)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_POITODELETE, poiToDelete)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        poiToDelete = savedInstanceState.getParcelable(ARG_POITODELETE) as PointOfInterest?
    }

    override fun onDeleteButtonPressed(dialog: DialogFragment, poiName: String) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onDeleteButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest()
            poiToDelete = pois.first { it.name == poiName } // It exists for sure.
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterestLocally(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onCancelDeletionButtonPressed")
        poiToDelete?.let {
            PointsOfInterest.addPointOfInterestLocally(it)
            dialog.dismiss()
        }
        poiToDelete = null
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onDeletionConfirmation")
        if(poiToDelete == null) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterest(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    override fun onPoiSelected(fragment: Fragment, poiName: String) {
        Log.v(TAG, "PointsOfInterestDialogListener.onPoiSelected")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest()
            val selectedPoi = pois.first { it.name == poiName } // It surely exists.
            val poiDetailsDialogFragment = PoiDetailsDialogFragment.newInstance(selectedPoi)
            poiDetailsDialogFragment.show(supportFragmentManager, "PoiDetailsDialogFragment")
        }
    }
}