package it.unibo.socialplaces.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import it.unibo.socialplaces.R
import it.unibo.socialplaces.databinding.FragmentPointsOfInterestBinding
import it.unibo.socialplaces.domain.PointsOfInterest
import it.unibo.socialplaces.fragment.dialog.pointsofinterest.EliminatePointOfInterestDialogFragment
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException


class PointsOfInterestFragment : Fragment(R.layout.fragment_points_of_interest) {
    // Listener
    interface PointsOfInterestListener {
        fun onPoiSelected(fragment: Fragment, poiName: String)
    }

    internal lateinit var listener: PointsOfInterestListener

    // UI
    private var _binding: FragmentPointsOfInterestBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var poisList: List<PointOfInterest> // TODO Sostituita con List da MutableList (tanto è comunque di tipo var)

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
            poisList = pArray?.let { p ->
                Log.d(TAG, "Loading poisList from savedInstanceState")
                List(p.size) { i -> p[i] as PointOfInterest }
            } ?: run {
                Log.e(TAG, "poisList inside savedInstanceState was null. Loading an emptyList.")
                emptyList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentPointsOfInterestBinding.bind(view)

        binding.noPoisItems.visibility = if(poisList.isEmpty()) View.VISIBLE else View.INVISIBLE

        binding.poisListView.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, poisList.map { it.name })

        binding.poisListView.setOnItemLongClickListener { parent, _, position, _ ->
            val selectedPoiName = parent.getItemAtPosition(position) as String
            val eliminatePoiDialog = EliminatePointOfInterestDialogFragment.newInstance(selectedPoiName)

            activity?.let {
                eliminatePoiDialog.show(it.supportFragmentManager, "EliminatePointOfInterestDialogFragment")
            }

            return@setOnItemLongClickListener true
        }

        binding.poisListView.setOnItemClickListener { parent, _, position, _ ->
            val selectedPoiName = parent.getItemAtPosition(position) as String
            listener.onPoiSelected(this, selectedPoiName)
        }

        binding.closePoisFragment.setOnClickListener {
            activity?.finish()
        }

        binding.refreshPoisListFragment.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                poisList = PointsOfInterest.getPointsOfInterest(forceSync = true)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.poisListView.adapter = ArrayAdapter(
                        view.context,
                        android.R.layout.simple_list_item_1,
                        poisList.map { it.name }
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        Log.v(TAG, "onAttach")
        super.onAttach(context)

        try {
            listener = context as PointsOfInterestListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement PointsOfInterestListener")
        }
    }
}