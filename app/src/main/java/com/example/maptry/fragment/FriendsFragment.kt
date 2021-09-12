package com.example.maptry.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.maptry.R
import com.example.maptry.databinding.FragmentFriendsBinding
import com.example.maptry.fragment.dialog.EliminateFriendDialogFragment
import com.example.maptry.fragment.dialog.FriendDialogFragment
import com.example.maptry.fragment.dialog.FriendPoiDialogFragment
import com.example.maptry.domain.Friends
import com.example.maptry.domain.PointsOfInterest
import com.example.maptry.model.friends.Friend
import com.example.maptry.model.pointofinterests.AddPointOfInterest
import com.example.maptry.model.pointofinterests.AddPointOfInterestPoi
import com.example.maptry.model.pointofinterests.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(R.layout.fragment_friends),
    EliminateFriendDialogFragment.EliminateFriendDialogListener,
    FriendPoiDialogFragment.FriendPoiDialogListener {

    // UI
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var friendsList: MutableList<Friend>
    private var removedFriend: Friend? = null
    private var friendPosition: Int? = null
    private var selectedFriendName: String? = null
    private var willDeleteFriend: Boolean? = null

    // API
    private val friends by lazy {
        Friends
    }

    private val pois by lazy {
        PointsOfInterest
    }

    companion object {
        private val TAG: String = FriendsFragment::class.qualifiedName!!

        private const val ARG_FRIENDSLIST = "friendsList"
        private const val ARG_REMOVEDFRIEND = "removedFriend"
        private const val ARG_FRIENDPOSITION = "friendPosition"
        private const val ARG_SELECTEDFRIENDNAME = "selectedFriendName"
        private const val ARG_WILLDELETEFRIEND = "willDeleteFriend"

        @JvmStatic
        fun newInstance(friends: List<Friend>, selectedFriendName: String) =
            FriendsFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArray(ARG_FRIENDSLIST, friends.toTypedArray())
                    putString(ARG_SELECTEDFRIENDNAME, selectedFriendName)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        arguments?.let {
            val pArray = it.getParcelableArray(ARG_FRIENDSLIST)
            pArray?.let { p ->
                Log.d(TAG, "Loading friendsList from savedInstanceState")
                friendsList = MutableList(p.size) { i -> p[i] as Friend }
            } ?: run {
                Log.e(TAG, "friendsList inside savedInstanceState was null. Loading an emptyList.")
                friendsList = emptyList<Friend>().toMutableList()
            }
            removedFriend = it.getParcelable(ARG_REMOVEDFRIEND)
            friendPosition = it.getInt(ARG_FRIENDPOSITION)
            selectedFriendName = it.getString(ARG_SELECTEDFRIENDNAME)
            willDeleteFriend = it.getBoolean(ARG_WILLDELETEFRIEND)
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
        Log.v(TAG, "EliminateFriendDialogListener.onDeleteButtonPressed")
        removedFriend = friendsList.first { it.friendUsername == selectedFriendName }
        friendPosition = friendsList.indexOf(removedFriend)
        willDeleteFriend = true // TODO Non necessario se tutto va correttamente
        friendsList.removeAt(friendPosition!!)

        arguments?.let {
            it.putParcelable(ARG_REMOVEDFRIEND, removedFriend)
            it.putInt(ARG_FRIENDPOSITION, friendPosition!!)
            it.putBoolean(ARG_WILLDELETEFRIEND, willDeleteFriend!!)
            it.putParcelableArray(ARG_FRIENDSLIST, friendsList.toTypedArray())
        }

        CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
    }

    override fun onCancelDeletionButtonPressed(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogListener.onCancelDeletionButtonPressed")
        willDeleteFriend = false
        friendsList.add(friendPosition!!, removedFriend!!)

        arguments?.let {
            it.putBoolean(ARG_WILLDELETEFRIEND, willDeleteFriend!!)
            it.putParcelableArray(ARG_FRIENDSLIST, friendsList.toTypedArray())
        }
    }

    override fun onDeletionConfirmation(dialog: DialogFragment) {
        Log.v(TAG, "EliminateFriendDialogListener.onDeletionConfirmation")
        if(!(willDeleteFriend!!)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            friends.removeFriend(removedFriend!!.friendUsername)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    override fun onAddButtonPressed(dialog: DialogFragment, poi: PointOfInterest) {
        Log.v(TAG, "FriendPoiDialogListener.onAddButtonPressed")
        CoroutineScope(Dispatchers.IO).launch {
            pois.addPointOfInterest(AddPointOfInterest(AddPointOfInterestPoi(poi)))
            pois.getPointsOfInterest(forceSync = true)
            CoroutineScope(Dispatchers.Main).launch { dialog.dismiss() }
        }
    }

    override fun onRouteButtonPressed(dialog: DialogFragment, address: String) {
        Log.v(TAG, "FriendPoiDialogListener.onRouteButtonPressed")
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