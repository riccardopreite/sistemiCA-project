package com.example.maptry.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.R
import com.example.maptry.domain.Friends
import com.example.maptry.fragment.FriendsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ListActivity: AppCompatActivity() {

    companion object {
        val TAG: String = ListActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(LoginActivity.TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        intent.extras?.let { extra ->
            when (extra.get("screen")){
                R.id.friends_list -> {
                    val fragmentName = "FriendsFragment"
                    CoroutineScope(Dispatchers.IO).launch {
                        val friendList = Friends.getFriends()
                        val listFragment = FriendsFragment.newInstance(friendList)

                        CoroutineScope((Dispatchers.Main)).launch {
                            supportFragmentManager.beginTransaction().apply {
                                replace(R.id.list_fragment, listFragment)
                                setReorderingAllowed(true)
                                addToBackStack(fragmentName)
                                commit()
                            }
                        }
                    }

                }
                R.id.pois_list -> {
                    print("POIS LIST WHEN")
                    //fragmentName = "PointsOfInterestFragment"
                }
                R.id.lives_list -> {
                    print("LIVES LIST WHEN")
                    //fragmentName = "LiveEventsFragment"
                }
                else -> {}
            }




        }
    }


}