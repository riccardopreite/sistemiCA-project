package it.unibo.socialplaces.activity.list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.PointsOfInterestFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.EliminatePointOfInterestDialogFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.PoiDetailsDialogFragment
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PointsOfInterestListActivity: it.unibo.socialplaces.activity.ListActivity(),
    EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener,
    PointsOfInterestFragment.PointsOfInterestDialogListener,
    PoiDetailsDialogFragment.PoiDetailsDialogListener {
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
        updateMarkerList()
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
            val pois = PointsOfInterest.getPointsOfInterest("",true)
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
            Log.w(TAG, "POIS Deletion canceled")

            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterest(it)
                CoroutineScope(Dispatchers.Main).launch {
                    dialog.dismiss()
                    updateMarkerList()
                }
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
    override fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "PoiDetailsDialogFragment.onShareButtonPressed")
        sharePlace(poi.name, poi.address, poi.latitude, poi.longitude)
    }

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "PoiDetailsDialogFragment.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    private fun sharePlace(name: String, address: String, latitude: Double, longitude: Double) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.share_place, name, address, latitude, longitude))
        }
        val createdIntent = Intent.createChooser(shareIntent,getString(R.string.share_place_intent, name))
        ContextCompat.startActivity(this, createdIntent, null)
    }

    private fun updateMarkerList(){
        Log.v(TAG,"PointsOfInterestListActivity.updatePOISList")
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest("",true)
            val poisFragment = PointsOfInterestFragment.newInstance(poisList)
            pushFragment(poisFragment)
        }
    }


}