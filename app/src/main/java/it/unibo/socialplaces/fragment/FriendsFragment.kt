package it.unibo.socialplaces.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import it.unibo.socialplaces.R
import it.unibo.socialplaces.databinding.FragmentFriendsBinding
import it.unibo.socialplaces.domain.Friends
import it.unibo.socialplaces.fragment.dialog.friends.EliminateFriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.FriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.AddFriendDialogFragment
import it.unibo.socialplaces.model.friends.Friend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FriendsFragment : Fragment(R.layout.fragment_friends){

    // UI
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var friendsList: MutableList<Friend>
    private var removedFriend: Friend? = null
    private var friendPosition: Int? = null
    private var selectedFriendName: String? = null
    private var willDeleteFriend: Boolean? = null

    companion object {
        private val TAG: String = FriendsFragment::class.qualifiedName!!

        private const val ARG_FRIENDSLIST = "friendsList"
        private const val ARG_REMOVEDFRIEND = "removedFriend"
        private const val ARG_FRIENDPOSITION = "friendPosition"
        private const val ARG_SELECTEDFRIENDNAME = "selectedFriendName"
        private const val ARG_WILLDELETEFRIEND = "willDeleteFriend"

        @JvmStatic
        fun newInstance(friends: List<Friend>) =
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

        binding.noFriendsItems.visibility = if(friendsList.isEmpty()) View.VISIBLE else View.INVISIBLE

        binding.friendsListView.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, friendsList.map { it.friendUsername })

        binding.friendsListView.setOnItemLongClickListener { parent, v, position, id ->
            val eliminateFriendDialog = EliminateFriendDialogFragment.newInstance(parent.getItemAtPosition(position) as String)

            activity?.let {
                eliminateFriendDialog.show(it.supportFragmentManager, "EliminateFriendDialogFragment")
            }

            return@setOnItemLongClickListener true
        }

        binding.friendsListView.setOnItemClickListener { parent, v, position, id ->
            val friendDialog = FriendDialogFragment.newInstance(parent.getItemAtPosition(position) as String)
            activity?.let {
                friendDialog.show(it.supportFragmentManager, "FriendDialogFragment")
            }
        }

        binding.addFriend.setOnClickListener {
            val addFriendDialog = AddFriendDialogFragment()
            activity?.let {
                addFriendDialog.show(it.supportFragmentManager, "AddFriendDialogFragment")
            }
        }

        binding.refreshFriends.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                friendsList.clear()
                friendsList.addAll(Friends.getFriends(true))
                CoroutineScope(Dispatchers.Main).launch {
                    binding.friendsListView.adapter = ArrayAdapter(
                        view.context,
                        android.R.layout.simple_list_item_1,
                        friendsList.map { it.friendUsername }
                    )
                }
            }

        }

        binding.closeFriendsFragment.setOnClickListener {
            activity?.finish()
        }
    }

    override fun onDestroyView() {
        Log.v(TAG, "onDestroyView")
        super.onDestroyView()
        _binding = null
    }
}