package com.example.maptry.fragment

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.databinding.DialogCustomEliminateBinding
import com.example.maptry.databinding.FragmentPointsOfInterestBinding
import com.example.maptry.dialog.EliminateFriendDialogFragment
import com.example.maptry.dialog.EliminatePointOfInterestDialogFragment
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.utils.deletePOI
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PointsOfInterestFragment : Fragment(R.layout.fragment_points_of_interest),
EliminatePointOfInterestDialogFragment.EliminatePointOfInterestDialogListener {
    private var _binding: FragmentPointsOfInterestBinding? = null
    private val binding get() = _binding!!

    private lateinit var poisList: MutableList<PointOfInterest>

    private var removedPoi: PointOfInterest? = null
    private var poiPosition: Int? = null
    private var selectedPoiName: String? = null
    private var willDeletePoi: Boolean? = null

    private val pois by lazy {
        PointsOfInterest
    }

    companion object {
        private val TAG: String = PointsOfInterestFragment::class.qualifiedName!!

        private const val ARG_POISLIST = "poisList"

        @JvmStatic
        fun newInstance(pois: List<PointOfInterest>) =
            PointsOfInterestFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(ARG_POISLIST, pois.toTypedArray())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArray = it.getParcelableArray(ARG_POISLIST)
            pArray?.let { p ->
                Log.d(TAG, "Loading poisList from savedInstanceState")
                poisList = MutableList(p.size) { i -> p[i] as PointOfInterest }
            } ?: run {
                Log.e(TAG, "poisList inside savedInstanceState was null. Loading an emptyList.")
                poisList = emptyList<PointOfInterest>().toMutableList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPointsOfInterestBinding.bind(view)

        binding.nosrc.visibility = if(poisList.isEmpty()) View.VISIBLE else View.INVISIBLE

        binding.lv.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, poisList.map { it.name })

        binding.lv.setOnItemLongClickListener { parent, v, position, id ->
            val eliminateFriendDialog = EliminatePointOfInterestDialogFragment()

            activity?.let {
                eliminateFriendDialog.show(it.supportFragmentManager, "EliminatePointOfInterestDialogFragment")
            }

            return@setOnItemLongClickListener true
        }

        binding.lv.setOnItemClickListener { parent, v, position, id ->
            val selectedPoiName = parent.getItemAtPosition(position) as String
            val poi = poisList.first { it.name == selectedPoiName }
            val markerId = LatLng(poi.latitude, poi.longitude)
            // TODO Sarebbe da invocare MapsActivity.onMarkerClick(mymarker[markerId]!!)
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onDeleteButtonPressed(dialog: DialogFragment) {
        removedPoi = poisList.first { it.name == selectedPoiName }
        poiPosition = poisList.indexOf(removedPoi)
        willDeletePoi = true // TODO Non necessario se tutto va correttamente
        poisList.removeAt(poiPosition!!)

        CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        willDeletePoi = false
        poisList.add(poiPosition!!, removedPoi!!)
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        if(!(willDeletePoi!!)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            pois.removePointOfInterest(removedPoi!!)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }
}