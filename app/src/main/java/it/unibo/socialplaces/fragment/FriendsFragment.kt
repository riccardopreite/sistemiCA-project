package it.unibo.socialplaces.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ArrayAdapter
import it.unibo.socialplaces.R
import it.unibo.socialplaces.databinding.FragmentFriendsBinding
import it.unibo.socialplaces.domain.Friends
import it.unibo.socialplaces.fragment.dialog.friends.EliminateFriendDialogFragment
import it.unibo.socialplaces.fragment.dialog.friends.AddFriendDialogFragment
import it.unibo.socialplaces.model.friends.Friend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ClassCastException

class FriendsFragment : Fragment(R.layout.fragment_friends) {
    // Listener
    interface FriendsListener {
        fun onFriendSelected(fragment: Fragment, friendName: String)
    }

    internal lateinit var listener: FriendsListener

    // UI
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = _binding!!

    // App state
    private lateinit var friendsList: List<Friend>

    companion object {
        private val TAG: String = FriendsFragment::class.qualifiedName!!

        private const val ARG_FRIENDSLIST = "friendsList"

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
            friendsList =  pArray?.let { p ->
                Log.d(TAG, "Loading friendsList from savedInstanceState")
                List(p.size) { i -> p[i] as Friend }
            } ?: run {
                Log.e(TAG, "friendsList inside savedInstanceState was null. Loading an emptyList.")
                emptyList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v(TAG, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFriendsBinding.bind(view)

        binding.noFriendsItems.visibility = if(friendsList.isEmpty()) View.VISIBLE else View.INVISIBLE

        binding.friendsListView.adapter = ArrayAdapter(view.context, android.R.layout.simple_list_item_1, friendsList.map { it.friendUsername })

        binding.friendsListView.setOnItemLongClickListener { parent, _, position, _ ->
            val selectedFriendName = parent.getItemAtPosition(position) as String
            val eliminateFriendDialog = EliminateFriendDialogFragment.newInstance(selectedFriendName)

            activity?.let {
                eliminateFriendDialog.show(it.supportFragmentManager, "EliminateFriendDialogFragment")
            }

            return@setOnItemLongClickListener true
        }

        binding.friendsListView.setOnItemClickListener { parent, _, position, _ ->
            val selectedFriendName = parent.getItemAtPosition(position) as String
            listener.onFriendSelected(this, selectedFriendName)
        }

        binding.addFriend.setOnClickListener {
            val addFriendDialog = AddFriendDialogFragment()
            activity?.let {
                addFriendDialog.show(it.supportFragmentManager, "AddFriendDialogFragment")
            }
        }

        binding.closeFriendsFragment.setOnClickListener {
            activity?.finish()
        }

        binding.refreshFriends.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                friendsList = Friends.getFriends(true)

                CoroutineScope(Dispatchers.Main).launch {
                    binding.friendsListView.adapter = ArrayAdapter(
                        view.context,
                        android.R.layout.simple_list_item_1,
                        friendsList.map { it.friendUsername }
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
            listener = context as FriendsListener
        } catch(e: ClassCastException) {
            throw ClassCastException("$context must implement FriendsListener")
        }
    }
}