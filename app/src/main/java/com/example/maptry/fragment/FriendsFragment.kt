package com.example.maptry.fragment

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.maptry.R
import com.example.maptry.databinding.DialogCustomEliminateBinding
import com.example.maptry.databinding.DialogCustomFriendPoiBinding
import com.example.maptry.databinding.DialogFriendViewBinding
import com.example.maptry.databinding.FragmentFriendsBinding
import com.example.maptry.dialog.EliminateFriendDialogFragment
import com.example.maptry.dialog.FriendDialogFragment
import com.example.maptry.dialog.FriendPoiDialogFragment
import com.example.maptry.domain.Friends
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(R.layout.fragment_friends),
    EliminateFriendDialogFragment.EliminateFriendDialogListener,
    FriendPoiDialogFragment.FriendPoiDialogListener {

    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    private lateinit var friendsList: MutableList<Friend>

    private var removedFriend: Friend? = null
    private var friendPosition: Int? = null
    private var selectedFriendName: String? = null
    private var willDeleteFriend: Boolean? = null

    private val friends by lazy {
        Friends
    }

    private val pois by lazy {
        PointsOfInterest
    }

    companion object {
        private val TAG: String = FriendsFragment::class.qualifiedName!!

        private const val ARG_FRIENDSLIST = "friendsList"

        @JvmStatic
        fun newInstance(friends: MutableList<Friend>) =
            FriendsFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(ARG_FRIENDSLIST, friends.toTypedArray())
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArray = it.getParcelableArray(ARG_FRIENDSLIST)
            pArray?.let { pArray ->
                Log.d(TAG, "Loading friendsList from savedInstanceState")
                friendsList = MutableList(pArray.size) { i -> pArray[i] as Friend }
            } ?: run {
                Log.e(TAG, "friendsList inside savedInstanceState was null. Loading an emptyList.")
                friendsList = emptyList<Friend>().toMutableList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFriendsBinding.bind(view)

        binding.nofriend.visibility = if(friendsList.isEmpty()) View.VISIBLE else View.INVISIBLE

        binding.fv.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, friendsList.map { it.friendUsername })

        binding.fv.setOnItemLongClickListener { parent, v, position, id ->
            val eliminateFriendDialog = EliminateFriendDialogFragment()

            activity?.let {
                eliminateFriendDialog.show(it.supportFragmentManager, "EliminateFriendDialogFragment")
            }

            return@setOnItemLongClickListener true
        }

        binding.fv.setOnItemClickListener { parent, v, position, id ->
            val friendDialog = FriendDialogFragment.newInstance(parent.getItemAtPosition(position) as String)
            activity?.let {
                friendDialog.show(it.supportFragmentManager, "FriendDialogFragment")
            }
        }
    }

    override fun onDeleteButtonPressed(dialog: DialogFragment) {
        removedFriend = friendsList.first { it.friendUsername == selectedFriendName }
        friendPosition = friendsList.indexOf(removedFriend)
        willDeleteFriend = true // TODO Non necessario se tutto va correttamente
        friendsList.removeAt(friendPosition!!)

        CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        willDeleteFriend = false
        friendsList.add(friendPosition!!, removedFriend!!)
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        if(!(willDeleteFriend!!)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            friends.removeFriend(removedFriend!!.friendUsername)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    override fun onAddButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        CoroutineScope(Dispatchers.IO).launch {
            pois.addPointOfInterest(AddPointOfInterest(AddPointOfInterestPoi(poi)))
            pois.getPointsOfInterest(forceSync = true)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$address"))
        dialog.dismiss()
        startActivity(intent)
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}