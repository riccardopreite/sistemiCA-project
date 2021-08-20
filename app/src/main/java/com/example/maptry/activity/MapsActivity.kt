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
import androidx.lifecycle.lifecycleScope
import com.example.maptry.*
import com.example.maptry.R
import com.example.maptry.R.id
import com.example.maptry.api.Retrofit
import com.example.maptry.changeUI.gson
import com.example.maptry.changeUI.markerView
import com.example.maptry.changeUI.showCreateMarkerView
import com.example.maptry.config.Auth
import com.example.maptry.dataclass.ConfirmRequest
import com.example.maptry.dataclass.FriendRequest
import com.example.maptry.location.myLocationClick
import com.example.maptry.location.registerLocationListener
import com.example.maptry.location.setUpMap
import com.example.maptry.location.startLocationUpdates
import com.example.maptry.model.friends.AddFriendshipRequest
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.PointOfInterest
import com.example.maptry.notification.NotifyService
import com.example.maptry.server.*
import com.example.maptry.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.http.HTTP
import java.io.IOException
import java.util.*
import kotlin.streams.toList

@Suppress("DEPRECATION", "DEPRECATED_IDENTITY_EQUALS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class MapsActivity: AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,NavigationView.OnNavigationItemSelectedListener {
    /**
     * Client for accessing the device position.
     */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * Set to true when requestCode == REQUEST_CHECK_SETTINGS and resultCode == Activity.RESULT_OK
     */
    private var locationUpdateState = false

    private var mainHandler = Handler()
    private var error = Runnable { println("error") }
    private var run = object : Runnable {
        override fun run() {
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
        val TAG: String = MapsActivity::class.qualifiedName!!

        lateinit var locationCallback: LocationCallback
        lateinit var lastLocation: Location
        @SuppressLint("StaticFieldLeak")
        lateinit var mapsActivityContext: Context
        lateinit var alertDialog: AlertDialog
        lateinit var mMap: GoogleMap // TODO Passare a Location.googleMap
        lateinit var mAnimation : Animation
        lateinit var geocoder : Geocoder

        /* Firestore stuff */
        lateinit var dataFromfirestore :List<DocumentSnapshot> // TODO Remove Firestore

        @SuppressLint("StaticFieldLeak")
        lateinit var db :FirebaseFirestore // TODO Remove Firestore
        /* End of Firestore stuff */

        /**
         * (UI) Reference to the drawer menu (opened).
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var drawerLayout: FrameLayout

        /**
         * (UI) Reference to the list of points of interest.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var listLayout: FrameLayout

        /**
         * (UI) Reference to the search bar on the screen top.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var homeLayout: FrameLayout

        /**
         * (UI) Reference to the splash screen.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var splashLayout: FrameLayout

        /**
         * (UI) Reference to the list of confirmed friends.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var friendLayout: FrameLayout

        /**
         * (UI) Reference to the popup for a new friend request.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var friendFrame: FrameLayout

        /**
         * (UI) Reference to the list of live events.
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var liveLayout: FrameLayout

        /**
         * Fragment manager.
         */
        lateinit var supportManager: FragmentManager


        private const val REQUEST_CHECK_SETTINGS = 2 // TODO Change with Location.REQUEST_CHECK_SETTINGS


        var builder = LocationSettingsRequest.Builder()
        var newBundy = Bundle()
        var mLocationRequest: LocationRequest? = null
        var isRunning : Boolean = false // TODO 1) Che serve?
        var zoom = 1
        var oldPos :Marker? = null
        var addrThread:Thread? = null
        var listAddr:MutableList<Address>? = null
        var drawed = false // TODO What do we need this for?
        // TODO FIRST Remove all the following
        var myjson = JSONObject() //tmp json

        /**
         * HashMap of Google Maps markers with key=LatLng::toString and value the LatLng object.
         */
        var mymarker = JSONObject() //marker

        /**
         * HashMap of PoIs and Live Events with key=LatLng::toString and value the json object of the point of interest or live event.
         */
        var myList = JSONObject() // POI json

        /**
         * List of points of interest of the user.
         */
        val poisList: MutableList<PointOfInterest> = emptyList<PointOfInterest>().toMutableList()

        /**
         * HashMap of Live Events with key=LatLng::toString and value the json object of the live event.
         */
        val myLive = JSONObject() // live json

        /**
         * List of live events of the user.
         */
        val liveEventsList: MutableList<LiveEvent> = emptyList<LiveEvent>().toMutableList()

        var friendJson = JSONObject() // friend json
        var friendTempPoi = JSONObject()

        // TODO SECOND Remove all the following that are a replacement of the previous ones

    }

    /*Start Initialize Function*/

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION ) {
                registerLocationListener()
            }
        }
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy")
        super.onDestroy()
        println("onDestroy distrutto")
        isRunning = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        if(isRunning) { // TODO 1) Che serve?
            return
        }
        isRunning = true
        mapsActivityContext = this
        // Loading view and setting reference to the UI components
        setContentView(R.layout.activity_maps)
        drawerLayout = findViewById(id.drawer_layout)
        listLayout = findViewById(id.list_layout)
        homeLayout = findViewById(id.homeframe)
        splashLayout = findViewById(id.splashFrame)
        friendLayout = findViewById(id.friend_layout)
        friendFrame = findViewById(id.friendFrame)
        liveLayout = findViewById(id.live_layout)

        // TODO Do we need a splash animation? Does the splashLayout actually get loaded?
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.enlarge)
        mAnimation.backgroundColor = Color.TRANSPARENT
        switchFrame(splashLayout,listOf(homeLayout,listLayout,drawerLayout,friendLayout,friendFrame,liveLayout))

        // TODO Do we really want to do this?
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        geocoder = Geocoder(this) // TODO Replace with Location.geocoder

        //create connection
        mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(run)

        // TODO It can be replaced with an authentication check instead that opening a new activity
        val menuIntent =  Intent(this, LoginActivity::class.java)
        val component = ComponentName(this, LoginActivity::class.java)
        intent.component = component
        startActivityForResult(menuIntent, LoginActivity.requestCodeSignIn)

        windowManager.defaultDisplay.getMetrics(DisplayMetrics())

        val mapFragment = supportFragmentManager
            .findFragmentById(id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("RestrictedApi")
    override fun onMapReady(googleMap: GoogleMap) {
        Log.v(TAG, "onMapReady")
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
            myLocationClick(ContextUtils.getActivity(mapsActivityContext))
            return@setOnMyLocationButtonClickListener true
        }

    }

    /*End Initialize Function*/

    /*Start Map Function*/

    /*This Function open a dialog with the information of the marker which was clicked*/
    @SuppressLint("SetTextI18n")
    override fun onMarkerClick(p0: Marker): Boolean {
        Log.v(TAG, "onMarkerClick")
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
        Log.v(TAG, "onMapClick")
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
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                Log.d(TAG, "onActivityResult: requestCode == REQUEST_CHECK_SETTINGS")
                if (resultCode == Activity.RESULT_OK) {
                    locationUpdateState = true
                }
            }
            30 -> {
                Log.d(TAG, "onActivityResult: requestCode == 30")
                if (resultCode == 60) {
                    println("SERVER OK")
                } else if (resultCode == 70) {
                    println("SERVER OK")
                }
            }
            1 -> {
                Log.d(TAG, "onActivityResult: requestCode == 1")
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
            LoginActivity.requestCodeSignIn -> {
                Log.d(TAG, "onActivityResult: requestCode == LoginActivity.requestCodeSignIn")
                if (resultCode == LoginActivity.resultCodeSignedIn) {
                    println("loggato")
                    // user logged, init structure, create user in firebase if not exist
                    Auth.signInAccount = GoogleSignIn.getLastSignedInAccount(this@MapsActivity)
                    val id: String = Auth.signInAccount?.email?.replace("@gmail.com", "")!!
                    val am: AccountManager = AccountManager.get(this)
                    val options = Bundle()
                    val new = Auth.signInAccount?.account
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

                } else if (resultCode == LoginActivity.resultCodeNotSignedIn) {
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
            else -> Unit
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.v(TAG, "onNavigationItemSelected")
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
        Log.v(TAG, "showPOI")
        show = {
//            var index = 0
            val txt: TextView = findViewById(id.nosrc)
            switchFrame(listLayout,listOf(homeLayout,drawerLayout,friendLayout,friendFrame,splashLayout,liveLayout))

            val lv:ListView = findViewById(R.id.lv)

            val len = poisList.size
            txt.visibility = if(len == 0) View.VISIBLE else View.INVISIBLE

            val arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, poisList.stream().map(PointOfInterest::name).toList())

            lv.setOnItemLongClickListener { parent, view, position, _ -> // _ refers to an id
                val inflater: LayoutInflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
                val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

                eliminateBtn.setOnClickListener {
                    val selectedItem = parent.getItemAtPosition(position) as String
                    deletePOI(selectedItem,view,show)
                }

                val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
//                dialogBuilder.setOnDismissListener { }
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
            /*
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
             */
        }
        show()
    }

    // populate ListView with live list
    fun showLive(){
        Log.v(TAG, "showLive")
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
    fun showFriend() {
        Log.v(TAG, "showFriend")
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

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val selectedItem = parent.getItemAtPosition(position) as String

                for(i in friendJson.keys()){
                    if(selectedItem == friendJson[i] as String) {
                        friendJson.remove(i)
                        val cancel = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = Auth.signInAccount?.email?.replace("@gmail.com","")
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
            val id = Auth.signInAccount?.email?.replace("@gmail.com", "")!!
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
        Log.v(TAG, "closeDrawer")
        println(view)
        if(drawerLayout.visibility == View.GONE) switchFrame(drawerLayout,listOf(homeLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
        else {
            reDraw()
            switchFrame(homeLayout,listOf(drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,liveLayout))
        }
    }

    //get email inserted to send a request via server
    fun addFriend(view: View) {
        Log.v(TAG, "addFriend")
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
            val userToAddEmail = emailText.text.toString()
            val personalEmail = Auth.signInAccount?.email
            if(userToAddEmail !="" && userToAddEmail != "Inserisci Email" && userToAddEmail != personalEmail && userToAddEmail != personalEmail?.replace("@gmail.com","")){
                val id = personalEmail?.replace("@gmail.com","")!!
                val receiver = userToAddEmail.replace("@gmail.com","")
                CoroutineScope(Dispatchers.IO).launch {
                    val response = try {
                        Retrofit.friendsApi.addFriend(AddFriendshipRequest(receiver, id))
                    } catch (e: IOException) {
                        e?.message?.let { it1 -> Log.e(TAG, it1) }
                        return@launch
                    } catch (e: HttpException) {
                        e?.message?.let { it1 -> Log.e(TAG, it1) }
                        return@launch
                    }

                    if(response.isSuccessful) {
                        Log.i(TAG, "Friend request successfully sent")
                    }

                    alertDialog.dismiss()
                }
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



