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
import it.unibo.socialplaces.fragment.PointsOfInterestListFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.EliminatePointOfInterestDialogFragment
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.PoiDetailsDialogFragment
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PointsOfInterestListActivity: it.unibo.socialplaces.activity.ListActivity(),
    EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener,
    PointsOfInterestListFragment.PointsOfInterestListener,
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
        updatePoisList()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_POITODELETE, poiToDelete)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        poiToDelete = savedInstanceState.getParcelable(ARG_POITODELETE) as PointOfInterest?
    }

    /**
     * @see EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener.onDeleteButtonPressed
     */
    override fun onDeleteButtonPressed(dialog: DialogFragment, poiName: String) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onDeleteButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest(forceSync = true)
            poiToDelete = pois.first { it.name == poiName } // It exists for sure.
            poiToDelete?.let {
                PointsOfInterest.removePointOfInterestLocally(it)
                CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
            }
        }
    }

    /**
     * @see EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener.onCancelDeletionButtonPressed
     */
    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogListener.onCancelDeletionButtonPressed")
        poiToDelete?.let {
            PointsOfInterest.addPointOfInterestLocally(it)
            dialog.dismiss()
        }
        poiToDelete = null
    }

    /**
     * @see EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener.onDeletionConfirmation
     */
    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener.onDeletionConfirmation")

        poiToDelete?.let { poi ->
            CoroutineScope(Dispatchers.IO).launch {
                PointsOfInterest.removePointOfInterest(poi)
                CoroutineScope(Dispatchers.Main).launch {
                    dialog.dismiss()
                    updatePoisList()
                }
            }
        } ?: run {
            Log.w(TAG, "Point of interest deletion cancelled.")
        }
    }

    /**
     * @see PointsOfInterestListFragment.PointsOfInterestListener.onPoiSelected
     */
    override fun onPoiSelected(fragment: Fragment, poiName: String) {
        Log.v(TAG, "PointsOfInterestListFragment.PointsOfInterestListener.onPoiSelected")
        CoroutineScope(Dispatchers.IO).launch {
            val pois = PointsOfInterest.getPointsOfInterest()
            val selectedPoi = pois.first { it.name == poiName } // It surely exists.
            CoroutineScope(Dispatchers.Main).launch {
                val poiDetailsDialogFragment = PoiDetailsDialogFragment.newInstance(selectedPoi)
                poiDetailsDialogFragment.show(supportFragmentManager, "PoiDetailsDialogFragment")
            }
        }
    }

    /**
     * @see PoiDetailsDialogFragment.PoiDetailsDialogListener.onShareButtonPressed
     */
    override fun onShareButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "PoiDetailsDialogFragment.PoiDetailsDialogListener.onShareButtonPressed")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            with(poi) {
                putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.share_place, name, address, latitude, longitude)
                )
            }
        }
        val createdIntent = Intent.createChooser(shareIntent,getString(R.string.share_place_intent, poi.name))
        ContextCompat.startActivity(this, createdIntent, null)
    }

    /**
     * @see PoiDetailsDialogFragment.PoiDetailsDialogListener.onRouteButtonPressed
     */
    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "PoiDetailsDialogFragment.PoiDetailsDialogListener.onRouteButtonPressed")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    /**
     * Retrieves the list of points of interest and pushes the [PointsOfInterestListFragment].
     */
    private fun updatePoisList() {
        Log.v(TAG,"updatePoisList")
        CoroutineScope(Dispatchers.IO).launch {
            val poisList = PointsOfInterest.getPointsOfInterest(forceSync = true)
            val poisFragment = PointsOfInterestListFragment.newInstance(poisList)
            pushFragment(poisFragment)
        }
    }
}