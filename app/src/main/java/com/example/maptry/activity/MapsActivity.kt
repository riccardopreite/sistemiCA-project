package com.example.maptry.activity

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.example.maptry.*
import com.example.maptry.R
import com.example.maptry.R.id
import com.example.maptry.changeUI.gson
import com.example.maptry.changeUI.markerView
import com.example.maptry.changeUI.showCreateMarkerView
import com.example.maptry.dataclass.ConfirmRequest
import com.example.maptry.dataclass.FriendRequest
import com.example.maptry.location.myLocationClick
import com.example.maptry.location.registerLocationListener
import com.example.maptry.location.setUpMap
import com.example.maptry.location.startLocationUpdates
import com.example.maptry.notification.NotifyService
import com.example.maptry.server.*
import com.example.maptry.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.material.internal.ContextUtils
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.util.*

@Suppress("DEPRECATION", "DEPRECATED_IDENTITY_EQUALS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class MapsActivity  : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,NavigationView.OnNavigationItemSelectedListener{

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationUpdateState = false
    private var mainHandler = Handler()
    private var error = Runnable { println("error") }
    private var run = object : Runnable {
        override fun run() {
            Log.v("MapsActivity", "run")
            if(drawed) { // wait firebase to load JSON or 1.5 sec
                switchFrame(homeLayout,listOf(listLayout,drawerLayout,friendLayout,friendFrame,splashLayout,liveLayout))
                mainHandler.removeCallbacksAndMessages(null)
                if(intent.hasExtra("lat") && intent.hasExtra("lon")){
                    val lat = intent.extras?.get("lat") as Double
                    val lon = intent.extras?.get("lon") as Double
                    val p0 = LatLng(lat,lon)
                    println(p0)
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            p0, 20F
                        )
                    )
                }
            }
            else {
                mainHandler.postDelayed(this, 1500)
            }
        }
    }
    private lateinit var show: () -> Unit
    companion object {
        lateinit var locationCallback: LocationCallback
        lateinit var firebaseAuth: FirebaseAuth
        lateinit var lastLocation: Location
        @SuppressLint("StaticFieldLeak")
        lateinit var context : Context
        lateinit var alertDialog: AlertDialog
        lateinit var mMap: GoogleMap
        lateinit var mAnimation : Animation
        lateinit var geocoder : Geocoder
        lateinit var dataFromfirestore :List<DocumentSnapshot>

        @SuppressLint("StaticFieldLeak")
        lateinit var db :FirebaseFirestore
        @SuppressLint("StaticFieldLeak")
        lateinit var drawerLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var listLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var homeLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var splashLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var friendLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var friendFrame: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var liveLayout: FrameLayout
        lateinit var supportManager: FragmentManager


        private const val REQUEST_CHECK_SETTINGS = 2
        const val REQUEST_LOCATION_PERMISSION = 1


        var builder = LocationSettingsRequest.Builder()
        var newBundy = Bundle()
        var mLocationRequest: LocationRequest? = null
        var ip = "casadiso.ddns.net"
//        var port = port+"" //oldport
        var port = ""
        var isRunning : Boolean = false
        var zoom = 1
        var oldPos :Marker? = null
        var addrThread:Thread? = null
        var listAddr:MutableList<Address>? = null
        var drawed = false
        var myjson = JSONObject() //tmp json
        var mymarker = JSONObject() //marker
        var myList = JSONObject() // POI json
        val myLive = JSONObject() // live json
        var account : GoogleSignInAccount? = null
        var friendJson = JSONObject() // friend json
        var friendTempPoi = JSONObject()

    }

    /*Start Initialize Function*/

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.v("MapsActivity", "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION ) {
                registerLocationListener()
            }
        }
    }

    override fun onDestroy() {
        Log.v("MapsActivity", "onDestroy")
        super.onDestroy()
        println("onDestroy distrutto")
        isRunning = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v("MapsActivity", "onCreate")
        super.onCreate(savedInstanceState)
        println("onCreate")
        if(isRunning) {
            return
        }
        setContentView(R.layout.activity_maps)
        isRunning = true
        context = this
        drawerLayout = findViewById(id.drawer_layout)
        listLayout = findViewById(id.list_layout)
        homeLayout = findViewById(id.homeframe)
        splashLayout = findViewById(id.splashFrame)
        friendLayout = findViewById(id.friend_layout)
        friendFrame = findViewById(id.friendFrame)
        liveLayout = findViewById(id.live_layout)

        mAnimation = AnimationUtils.loadAnimation(this, R.anim.enlarge)
        mAnimation.backgroundColor = Color.TRANSPARENT
        switchFrame(splashLayout,listOf(homeLayout,listLayout,drawerLayout,friendLayout,friendFrame,liveLayout))

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        geocoder = Geocoder(this)

        //create connection
        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(run)

        // start intent to log in user
        val menuIntent =  Intent(this, LoginActivity::class.java)
        val component = ComponentName(this, LoginActivity::class.java)
        intent.component = component
        startActivityForResult(menuIntent,40)

        val displayMetrics = DisplayMetrics()

        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val mapFragment = supportFragmentManager
            .findFragmentById(id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("RestrictedApi")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.v("MapsActivity", "onMapReady")
        mMap = googleMap
        supportManager = supportFragmentManager
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)
        startLocationUpdates()
        setUpMap(getString(R.string.google_maps_key))
        val navMenu: NavigationView = findViewById(id.nav_view)
        navMenu.setNavigationItemSelectedListener(this)
//        setUpSearch()
        // override methode to ask to turn on GPS if is off or to move Camera if is on
        mMap.setOnMyLocationButtonClickListener {
            myLocationClick(ContextUtils.getActivity(context))
            return@setOnMyLocationButtonClickListener true
        }

    }

    /*End Initialize Function*/

    /*Start Map Function*/

    /*This Function open a dialog with the information of the marker which was clicked*/
    @SuppressLint("SetTextI18n")
    override fun onMarkerClick(p0: Marker): Boolean {
        Log.v("MapsActivity", "onMarkerClick")
        try {
            val myPos = LatLng(lastLocation.latitude, lastLocation.longitude)

            if (p0.position == myPos) {
                onMapClick(myPos)
                return true
            }
        }
        catch(e:Exception){
            println("GPS OFF")
        }
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView = markerView(inflater,p0)

        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogView)

        alertDialog = dialogBuilder.create()
        alertDialog.show()
        return false
    }

    /*Open Dialog to create new POI in the position clicked*/
    @SuppressLint("SetTextI18n")
    override fun onMapClick(p0: LatLng) {
        Log.v("MapsActivity", "onMapClick")
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView = showCreateMarkerView(inflater,p0)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    /*End Map Function*/


    /*Start Activity for result Function*/
    @SuppressLint("ResourceType", "CutPasteId")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v("MapsActivity", "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
            }
        }
        else if (requestCode == 30) {
            if (resultCode == 60) {
                println("SERVER OK")
            }
            else if (resultCode == 70) {
                println("SERVER OK")
            }
        }
        else if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val place = data?.let { Autocomplete.getPlaceFromIntent(it) }
                Log.i("OK", "Place: " + place?.name + ", " + place?.id)
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = data?.let { Autocomplete.getStatusFromIntent(it) }
                if (status != null) {
                    Log.i("error", status.statusMessage)
                }
            } else if (resultCode == RESULT_CANCELED) {
                println("Result canceled")
                // The user canceled the operation.
            }
        }
        else if (requestCode == 40) {
            if (resultCode == 50) {
                println("loggato")
                // user logged, init structure, create user in firebase if not exist
                account = GoogleSignIn.getLastSignedInAccount(this@MapsActivity)
                val id: String = account?.email?.replace("@gmail.com", "")!!
                val am: AccountManager = AccountManager.get(this)
                val options = Bundle()
                val new = account?.account
                val letHandler = Handler(Looper.getMainLooper())
                letHandler.post(error)

//                am.getAuthToken(
//                    new,                     // Account retrieved using getAccountsByType()
//                    "Manage your tasks",            // Auth scope
//                    options,                        // Authenticator-specific options
//                    this,                           // Your activity
//                    OnTokenAcquired(),              // Callback called when a token is successfully acquired
//                    letHandler            // Callback called if an error occurs
//                )
                isRunning = true

                // QUA SI USA FIREBASE
                FirebaseFirestore.setLoggingEnabled(true)
                db = FirebaseFirestore.getInstance()
                val docRef = db.collection("user")
                if (!db.document("user/$id").get().isSuccessful) {
                    docRef.document(id).set({})
                }
//                checkUser(id)

                val intent = Intent(this, NotifyService::class.java)
                startService(intent)
                createPoiList(id)
                createFriendList(id)
                createLiveList(id)


                val navBar = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
                println("SHOW HOME")
                setHomeLayout(navBar)

            }
            else if (resultCode == 40) {
                println("non loggato")
                val x = findViewById<NavigationView>(id.nav_view).getHeaderView(0)
                val close = x.findViewById<ImageView>(R.id.close)
                val user = x.findViewById<TextView>(R.id.user)
                val email = x.findViewById<TextView>(R.id.email)
                val imageView = x.findViewById<ImageView>(R.id.imageView)
                close.visibility = View.GONE
                imageView.visibility = View.GONE
                user.visibility = View.GONE
                email.visibility = View.GONE
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v("MapsActivity", "onNavigationItemSelected")
        println("MENU")
        return when (item.itemId) {
            id.list -> {
                showPOI()
                return true
            }
            id.friend ->{
                showFriend()
                return true
            }
            id.live ->{
                showLive()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }

    }
    
    /*End Activity for result Function*/

    /*Start Utils Function*/

    // populate ListView with poi list
    @SuppressLint("WrongViewCast")
    fun showPOI() {
        Log.v("MapsActivity", "showPOI")
        show = {
            var index = 0
            val txt: TextView = findViewById(id.nosrc)
            switchFrame(listLayout,listOf(homeLayout,drawerLayout,friendLayout,friendFrame,splashLayout,liveLayout))

            val lv:ListView = findViewById(R.id.lv)
            var len = 0
            for (i in myList.keys()){
                if(myList.getJSONObject(i).has("type") && myList.getJSONObject(i).get("type") as String != "Live") len++
            }

            val userList = MutableList(len) { "" }
            if(len == 0) txt.visibility = View.VISIBLE
            else txt.visibility = View.INVISIBLE
            for (i in myList.keys()){
                if(myList.getJSONObject(i).has("type") && myList.getJSONObject(i).get("type") as String != "Live"){
                    userList[index] = myList.getJSONObject(i).get("name") as String
                    index++
                }
            }
            val arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList)

            lv.setOnItemLongClickListener { parent, view, position, _ -> //last elem was id

                val inflater: LayoutInflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
                val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

                eliminateBtn.setOnClickListener {
                    val selectedItem = parent.getItemAtPosition(position) as String
                    deletePOI(selectedItem,view,show)

                }
                val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
                dialogBuilder.setOnDismissListener { }
                dialogBuilder.setView(dialogView)

                alertDialog = dialogBuilder.create()
                alertDialog.show()


                return@setOnItemLongClickListener true
            }
            lv.setOnItemClickListener { parent, _, position, _ -> //view e id
                val selectedItem = parent.getItemAtPosition(position) as String
                for (i in myList.keys()){
                    if(selectedItem == myList.getJSONObject(i).get("name") as String)
                        onMarkerClick(mymarker[i] as Marker)
                }
            }
            lv.adapter = arrayAdapter
        }
        show()
    }

    // populate ListView with live list
    fun showLive(){
        Log.v("MapsActivity", "showLive")
        val len = myLive.length()
        var index = 0
        val txt: TextView = findViewById(id.nolive)
        switchFrame(liveLayout,listOf(listLayout,homeLayout,drawerLayout,friendLayout,friendFrame,splashLayout))

        val  lv:ListView = findViewById(id.lvLive)
        val userList = MutableList(len) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in myLive.keys()){
            userList[index] = myLive.getJSONObject(i).get("name") as String
            index++
        }

        val  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList)

        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val selectedItem = parent.getItemAtPosition(position) as String
            for (i in myLive.keys()){
                if(selectedItem == myLive.getJSONObject(i).get("name") as String)
                    onMarkerClick(mymarker[i] as Marker)
            }
        }
        lv.adapter = arrayAdapter
    }

    // populate ListView with friend list
    @SuppressLint("ShowToast")
    fun showFriend(){
        Log.v("MapsActivity", "showFriend")
        println("friendJson")
        println(friendJson)
        val len = friendJson.length()
        var index = 0
        val txt: TextView = findViewById(id.nofriend)
        switchFrame(friendLayout,listOf(listLayout,homeLayout,drawerLayout,friendFrame,splashLayout,liveLayout))


        val  lv:ListView = findViewById(id.fv)
        val friendList = MutableList(len) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in friendJson.keys()){
            friendList[index] = friendJson[i] as String
            index++
        }

        val  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendList)
        lv.setOnItemLongClickListener { parent, view, position, _ -> //id
            Log.v("MapsActivity", "lv.setOnItemLongClickListener")
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val selectedItem = parent.getItemAtPosition(position) as String
                Log.v("MapsActivity", "eliminateBtn.setOnClickListener")
                for(i in friendJson.keys()){
                    if(selectedItem == friendJson[i] as String) {
                        friendJson.remove(i)
                        val cancel = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            snackbar.setAction(cancel) {
                                // Toast to undo operation
                                id?.let { _ -> //it1
                                    friendJson.put(i, selectedItem)
                                    val confirm = ConfirmRequest(id, selectedItem)
                                    val jsonToAdd = gson.toJson(confirm)
                                    confirmFriend(jsonToAdd)
                                    Toast.makeText(this, "Annulata rimozione di $selectedItem", Toast.LENGTH_LONG)
                                    showFriend()

                                }
                            }
                        snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                            }
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                if (id != null) {
                                    val remove = FriendRequest(selectedItem,id)
                                    val jsonToRemove = gson.toJson(remove)
                                    removeFriend(jsonToRemove)
                                }
                            }
                        })
                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        showFriend()
                        alertDialog.dismiss()
                        break
                    }
                }

            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create()
            alertDialog.show()
            return@setOnItemLongClickListener true
        }

        lv.setOnItemClickListener { parent, _, position, _ -> //wiew e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_friend_view, null)
            val txtName :TextView = dialogView.findViewById(id.friendNameTxt)
            val spinner :Spinner = dialogView.findViewById(id.planets_spinner_POI)
            val selectedItem = parent.getItemAtPosition(position) as String

            val context = this
            txtName.text = selectedItem
            // ask public friend's poi with a server call
            val id = account?.email?.replace("@gmail.com", "")!!
            val result: JSONObject = getPoiFromFriend(id,selectedItem)
            println(result)
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            val alertDialog2 = dialogBuilder.create()
            alertDialog2.show()
            val length = result.length()
            val markerList = MutableList(length + 1) { "" }
            var indexMarkerMap = 1
            // add this empty item cause a bug with spinner, on init select the first item and trigger the onItemSelected
            markerList[0] = ""
            for (i in result.keys()) {

                if (result.getJSONObject(i).get("visibility") as String == "Pubblico") {
                    markerList[indexMarkerMap] =
                        result.getJSONObject(i).get("name") as String
                    indexMarkerMap++
                }
            }
            val arrayAdapter2: ArrayAdapter<String> = ArrayAdapter<String>(
                context,
                R.layout.support_simple_spinner_dropdown_item, markerList
            )
            spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (parent?.getItemAtPosition(position) as String != "") {
                            var key = ""
                            val selectedMarker =
                                parent.getItemAtPosition(position) as String
                            var lat = 0.0
                            var lon = 0.0
                            for (i in result.keys()) {
                                if (result.getJSONObject(i)
                                        .get("name") == selectedMarker
                                ) {
                                    key = i
                                    lat = result.getJSONObject(i).get("latitude")
                                        .toString()
                                        .toDouble()
                                    lon = result.getJSONObject(i).get("longitude")
                                        .toString()
                                        .toDouble()
                                }
                            }
                            val pos = LatLng(
                                lat,
                                lon
                            )
                            val mark = createMarker(pos)
                            friendTempPoi.put(
                                pos.toString(),
                                result.getJSONObject(key)
                            )
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lat,
                                        lon
                                    ), 20F
                                )
                            )
                            switchFrame(
                                homeLayout,
                                listOf(friendLayout,
                                    listLayout,
                                    drawerLayout,
                                    friendFrame,
                                    splashLayout,
                                    liveLayout)
                            )
                            alertDialog2.dismiss()
                            showPOIPreferences(
                                pos.toString(),
                                inflater,
                                context,
                                mark!!
                            )
                        }
                    }
                }
            spinner.adapter = arrayAdapter2
        }
        lv.adapter = arrayAdapter
    }

    // show menu or home and reDraw all poi
    fun closeDrawer(view: View) {
        Log.v("MapsActivity", "closeDrawer")
        println(view)
        if(drawerLayout.visibility == View.GONE) switchFrame(drawerLayout,listOf(homeLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
        else {
            reDraw()
            switchFrame(homeLayout,listOf(drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
        }
    }
    //get email inserted to send a request via server
    fun addFriend(view: View) {
        Log.v("MapsActivity", "addFriend")
        println(view)
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.add_friend, null)
        val emailText : EditText = dialogView.findViewById(id.friendEmail)
        val addBtn: Button = dialogView.findViewById(id.friendBtn)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create()
        alertDialog.show()

        addBtn.setOnClickListener {
            if(emailText.text.toString() !="" && emailText.text.toString() != "Inserisci Email" && emailText.text.toString() != account?.email && emailText.text.toString() != account?.email?.replace("@gmail.com","")){
                val id = account?.email?.replace("@gmail.com","")!!
                val receiver = emailText.text.toString().replace("@gmail.com","")
                val sendRequest = FriendRequest(receiver,id)
                val jsonToAdd = gson.toJson(sendRequest)
                sendFriendRequest(jsonToAdd)

                alertDialog.dismiss()
            }
        }
    }

    /*End Utils Function*/

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onSaveInstanceState(newBundy)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", newBundy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getBundle("newBundy")
    }
}



