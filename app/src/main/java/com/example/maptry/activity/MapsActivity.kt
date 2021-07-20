package com.example.maptry.activity

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maptry.*
import com.example.maptry.R
import com.example.maptry.R.id
import com.example.maptry.changeUI.CircleTransform
import com.example.maptry.notification.NotifyService
import com.example.maptry.server.*
import com.example.maptry.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.internal.ContextUtils.getActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
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
            if(drawed) { // wait firebase to load JSON or 1.5 sec
                switchFrame(homeLayout,listLayout,drawerLayout,friendLayout,friendFrame,splashLayout,carLayout,liveLayout,loginLayout)
                mainHandler.removeCallbacksAndMessages(null)
            }
            else {
                mainHandler.postDelayed(this, 1500)
            }
        }
    }

    companion object {
        lateinit var locationCallback: LocationCallback
        lateinit var firebaseAuth: FirebaseAuth
        lateinit var lastLocation: Location
        @SuppressLint("StaticFieldLeak")
        lateinit var context : Context
        lateinit var alertDialog: AlertDialog
        lateinit var mMap: GoogleMap
        lateinit var mAnimation : Animation
        //        lateinit var dataFromfirebase: DataSnapshot
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
        lateinit var carLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var liveLayout: FrameLayout
        @SuppressLint("StaticFieldLeak")
        lateinit var loginLayout: FrameLayout


        private const val UPDATE_INTERVAL = (10 * 1000).toLong()  /* 10 secs */
        private const val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
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
        val myCar = JSONObject() // car json
        val myLive = JSONObject() // live json
        var account : GoogleSignInAccount? = null
        var friendJson = JSONObject() // friend json
        var friendTempPoi = JSONObject()

    }

    /*Start Initialize Function*/
    private fun startLocationUpdates() {
        // initialize location request object
        mLocationRequest = LocationRequest.create()
        mLocationRequest!!.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
            setFastestInterval(FASTEST_INTERVAL)
        }

        // initialize location setting request builder object
        builder.addLocationRequest(mLocationRequest!!)
        builder.setAlwaysShow(true)
        val locationSettingsRequest = builder.build()

        // initialize location service object
        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        // call register location listener
        registerLocationListner()
    }
    private fun registerLocationListner() {
        // initialize location callback object
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                onLocationChanged(locationResult!!.lastLocation)
            }
        }
    }
    // move position marker
    private fun onLocationChanged(location: Location) {

        val x = LatLng(location.latitude, location.longitude)
        try{
            oldPos?.remove()
        }
        catch (e:Exception){
            println("first time")
        }
        oldPos = createMarker(x)
        mymarker.remove(oldPos?.position.toString())

        if(zoom == 1){
            lastLocation = location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(x, 17F))
            zoom = 0
        }
        lastLocation = location
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1) {
            if (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION ) {
                registerLocationListner()
            }
        }
    }


    // init Map
    private fun setUpMap() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        val mapFragment =  supportFragmentManager.findFragmentById(id.map) as SupportMapFragment
        var locationButton : View? = mapFragment.view?.findViewById<LinearLayout>(Integer.parseInt("1"))
        val prov : View = (locationButton?.parent) as View
        locationButton = prov.findViewById(Integer.parseInt("2"))
        val layoutParams = locationButton?.layoutParams as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 0, 30, 30)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy distrutto")
        isRunning = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
        carLayout = findViewById(id.car_layout)
        liveLayout = findViewById(id.live_layout)
        loginLayout = findViewById(id.login_layout)

        mAnimation = AnimationUtils.loadAnimation(this, R.anim.enlarge)
        mAnimation.backgroundColor = Color.TRANSPARENT
        switchFrame(splashLayout,homeLayout,listLayout,drawerLayout,friendLayout,friendFrame,carLayout,liveLayout,loginLayout)

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        geocoder = Geocoder(this)

        //create connectioni
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
        mMap = googleMap
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)
        startLocationUpdates()
        setUpMap()
        setUpSearch()
        // override methode to ask to turn on GPS if is off or to move Camera if is off
        mMap.setOnMyLocationButtonClickListener {

            val provider: String = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED
            )

            if (!provider.contains("gps")) { //if gps is disabled
                val result: Task<LocationSettingsResponse> =
                    getActivity(context)?.let {
                        LocationServices.getSettingsClient(it)
                            .checkLocationSettings(builder.build())
                    } as Task<LocationSettingsResponse>



                result.addOnCompleteListener { task ->
                    try {
                        val response: LocationSettingsResponse? =task.getResult(ApiException::class.java)
                        println(response)
                    } catch (exception: ApiException) {
                        when (exception.statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                             // Location settings are not satisfied. But could be fixed by showing the
                                try {
                                    // Cast to a resolvable exception.
                                    val resolvable: ResolvableApiException =
                                        exception as ResolvableApiException
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    resolvable.startResolutionForResult(
                                        getActivity(context),
                                        LocationRequest.PRIORITY_HIGH_ACCURACY
                                    )
                                } catch (e: IntentSender.SendIntentException) {
                                    // Ignore the error.
                                } catch (e: ClassCastException) {
                                    // Ignore, should be an impossible error.
                                }
                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            }
                        }
                    }
                }
            }
            else{
                try{
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(
                        lastLocation.latitude,
                        lastLocation.longitude), 20F))
                }
                catch (e:Exception){}
            }
            return@setOnMyLocationButtonClickListener true
        }

    }

    /*End Initialize Function*/


    /*Start Map Function*/

    /*This Function open a dialog with the information of the marker which was clicked*/
    @SuppressLint("SetTextI18n")
    override fun onMarkerClick(p0: Marker): Boolean {

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
        val dialogView: View = inflater.inflate(R.layout.dialog_custom_view, null)
        val address: TextView = dialogView.findViewById(id.txt_addressattr)
        val phone: TextView = dialogView.findViewById(id.phone_contentattr)
        val phoneCap: TextView = dialogView.findViewById(id.phone_content)
        val header: TextView = dialogView.findViewById(id.headerattr)
        val url: TextView = dialogView.findViewById(id.uri_lblattr)
        val urlCap: TextView = dialogView.findViewById(id.uri_lbl)
        val text : String =  myList.getJSONObject(p0.position.toString()).get("cont") as String+": "+ myList.getJSONObject(p0.position.toString()).get("name") as String
        header.text =  text
        address.text = myList.getJSONObject(p0.position.toString()).get("addr") as String
        url.text = myList.getJSONObject(p0.position.toString()).get("url") as String
        phone.text = myList.getJSONObject(p0.position.toString()).get("phone") as String
        when {
            myList.getJSONObject(p0.position.toString()).get("cont") as String == "Live" -> {
                phone.text = myLive.getJSONObject(p0.position.toString()).get("timer") as String + " minuti"
                phoneCap.text = "Timer"
                url.text = myLive.getJSONObject(p0.position.toString()).get("owner") as String
                urlCap.text = "Proprietario"

            }
            myList.getJSONObject(p0.position.toString()).get("cont") as String == "Macchina" -> {
                phone.text = myCar.getJSONObject(p0.position.toString()).get("timer") as String + " minuti"
                phoneCap.text = "Timer"
                urlCap.text = "Proprietario"
                url.text = myCar.getJSONObject(p0.position.toString()).get("owner") as String
            }
            else -> {
                phoneCap.text = "N.cellulare"
                urlCap.text = "WebSite"
            }
        }

        val routebutton: Button = dialogView.findViewById(id.routeBtn)
        val removebutton: Button = dialogView.findViewById(id.removeBtnattr)
        removebutton.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type="text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com/?q="+ myList.getJSONObject(p0.position.toString()).get("lat")+","+ myList.getJSONObject(p0.position.toString()).get("lon"))
            startActivity(Intent.createChooser(shareIntent,"Stai condividendo "+ myList.getJSONObject(p0.position.toString()).get("name")))
            alertDialog.dismiss()
        }

        if (homeLayout.visibility == View.GONE) {
            routebutton.text = "Visualizza"
            routebutton.setOnClickListener {
                mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            p0.position.latitude,
                            p0.position.longitude
                        ), 20F
                    )
                )
                switchFrame(homeLayout,listLayout,drawerLayout,friendLayout,friendFrame,carLayout,splashLayout,liveLayout,loginLayout)
                alertDialog.dismiss()
            }
        }
        else{
            routebutton.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
                startActivity(intent)
                alertDialog.dismiss()
            }
        }

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
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_list_view, null)
        val spinner: Spinner = dialogView.findViewById(id.planets_spinner)
        val lname : EditText = dialogView.findViewById(id.txt_lname)
        val address :  TextView = dialogView.findViewById(id.txt_address)
        val publicButton: RadioButton = dialogView.findViewById(id.rb_public)
        val privateButton: RadioButton = dialogView.findViewById(id.rb_private)
        val timePickerLayout = dialogView.findViewById<RelativeLayout>(id.timePicker)
        val timePicker = dialogView.findViewById<TimePicker>(id.timePicker1)
        val addbutton: Button = dialogView.findViewById(id.addBtn)
        val removebutton: Button = dialogView.findViewById(id.removeBtn)
        val id = account?.email?.replace("@gmail.com", "")

        timePicker.hour = 3
        timePicker.minute = 0
        val radioGroup = dialogView.findViewById<RelativeLayout>(R.id.rl_gender)
        var time : Int
        address.isEnabled = false

        val background = object : Runnable {
            override fun run() {
                try {
                    listAddr = geocoder.getFromLocation(p0.latitude, p0.longitude, 1)
                    return
                } catch (e: IOException) {
                    Log.e("Error", "grpc failed: " + e.message, e)
                }
            }
        }
        addrThread = Thread(background)
        addrThread?.start()
        try {
            addrThread?.join()
        } catch (e:InterruptedException) {
            e.printStackTrace()
        }
        address.text = listAddr?.get(0)?.getAddressLine(0)
        println(listAddr?.get(0))
        println(listAddr?.get(0)?.phone)
        println(listAddr?.get(0)?.url)

        ArrayAdapter.createFromResource(
            this,
            R.array.planets_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }


        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
            // show timepicker for car and live
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val type = parent?.getItemAtPosition(position) as String
                if(type == "Macchina" || type == "Live"){
                    radioGroup.visibility = View.GONE
                    timePicker.setIs24HourView(true)
                    timePickerLayout.visibility = View.VISIBLE
                }
                else{
                    radioGroup.visibility = View.VISIBLE
                    timePicker.setIs24HourView(true)
                    timePickerLayout.visibility = View.GONE
                }
            }
        }
        removebutton.setOnClickListener {
            alertDialog.dismiss()
        }
        addbutton.setOnClickListener {
            val text = lname.text.toString()
            if (text == "") {
                lname.background.setColorFilter(
                    resources.getColor(R.color.quantum_googred),
                    PorterDuff.Mode.SRC_ATOP
                )
            }
            else {
                myjson = JSONObject()
                var gender = "gen"
                if (publicButton.isChecked)
                    gender = publicButton.text.toString()
                if (privateButton.isChecked)
                    gender = privateButton.text.toString()

                when {
                    spinner.selectedItem.toString() == "Macchina" -> {
                        for (i: String in myCar.keys()) {
                            try {
                                val x: String = myCar.getJSONObject(i).get("name") as String
                                if (text == x) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener

                                }
                                if (address.text == myCar.getJSONObject(i).get("addr") as String) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener
                                }
                            } catch (e: java.lang.Exception) {
                                println("ops")
                            }
                        }
                        gender = privateButton.text.toString()
                        time = timePicker.hour * 60 + timePicker.minute
                        val marker = createMarker(p0)
                        marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        myjson.put("type", "Privato")
                        myjson.put("timer", time.toString())
                        myjson.put("name", text)
                        myjson.put("addr", address.text.toString())
                        myjson.put("owner", account?.email?.replace("@gmail.com", ""))
                        myjson.put("marker", marker)
                        myjson.put("cont", spinner.selectedItem.toString())
                        myjson.put("url", "da implementare")
                        myjson.put("phone", "da implementare")

                        resetTimerAuto(myjson)
                        myCar.put(p0.toString(), myjson)
                        myList.put(p0.toString(), myjson)

                        id?.let { it1 ->
                            if (marker != null) {
                                writeNewCar(
                                    it1,
                                    text,
                                    address.text.toString(),
                                    time.toString(),
                                    it1,
                                    marker,
                                    "da implementare",
                                    "da implementare",
                                    "Privato",
                                    "Macchina"
                                )
                            }
                        }
                    }
                    spinner.selectedItem.toString() == "Live" -> {
                        for (i: String in myLive.keys()) {
                            try {
                                val x: String = myLive.getJSONObject(i).get("name") as String
                                if (text == x) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener

                                }
                                if (address.text == myLive.getJSONObject(i).get("addr") as String) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener
                                }
                            } catch (e: java.lang.Exception) {
                                println("ops")
                            }
                        }
                        time = timePicker.hour * 60 + timePicker.minute
                        val marker = createMarker(p0)
                        marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

                        myjson.put("type", "Pubblico")
                        myjson.put("timer", time.toString())
                        myjson.put("name", text)
                        myjson.put("addr", address.text.toString())
                        myjson.put("owner", account?.email?.replace("@gmail.com", ""))
                        myjson.put("marker", marker)
                        myjson.put("cont", spinner.selectedItem.toString())
                        myjson.put("url", "da implementare")
                        myjson.put("phone", "da implementare")
                        startLive(myjson)
                        myLive.put(p0.toString(), myjson)
                        myList.put(p0.toString(), myjson)

                        id?.let { it1 ->
                            if (marker != null) {
                                writeNewLive(
                                    it1,
                                    text,
                                    address.text.toString(),
                                    time.toString(),
                                    it1,
                                    marker,
                                    "da implementare",
                                    "da implementare",
                                    "Pubblico",
                                    "Live"
                                )
                            }
                        }
                    }
                    //                spinner on item selected
                    else -> {
                        for (i: String in myList.keys()) {
                            try {
                                val x: String = myList.getJSONObject(i).get("name") as String
                                if (text == x) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener

                                }
                                if (address.text == myList.getJSONObject(i).get("addr") as String) {
                                    lname.background.setColorFilter(
                                        resources.getColor(R.color.quantum_googred),
                                        PorterDuff.Mode.SRC_ATOP
                                    )
                                    return@setOnClickListener
                                }
                            } catch (e: java.lang.Exception) {
                                println("ops")
                            }
                        }
                        val marker = createMarker(p0)
                        marker?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

                        myjson.put("name", text)
                        myjson.put("addr", address.text.toString())
                        myjson.put("cont", spinner.selectedItem.toString())
                        myjson.put("type", gender)
                        myjson.put("marker", marker)
                        myjson.put("url", "da implementare")
                        myjson.put("phone", "da implementare")
                        if(listAddr?.get(0)?.url === null || listAddr?.get(0)?.url === "" || listAddr?.get(0)?.url === " ") myjson.put("url","Url non trovato")
                        else  myjson.put("url", listAddr?.get(0)?.url)
                        if(listAddr?.get(0)?.phone === null|| listAddr?.get(0)?.phone === "" || listAddr?.get(0)?.phone === " ") myjson.put("phone","cellulare non trovato")
                        else  myjson.put("phone", listAddr?.get(0)?.phone)
                        myList.put(p0.toString(), myjson)
                        id?.let { it1 ->
                            if (marker != null) {
                                writeNewPOI(
                                    it1,
                                    text,
                                    address.text.toString(),
                                    spinner.selectedItem.toString(),
                                    gender,
                                    marker,
                                    "da implementare",
                                    "da implementare"
                                )
                            }
                        }
                    }
                }
                alertDialog.dismiss()
            }
        }
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener { }
        dialogBuilder.setView(dialogView)

        alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    /*End Map Function*/



    /*Start Override Function*/
    @SuppressLint("ResourceType")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
                val id: String? = account?.email?.replace("@gmail.com", "")
                val am: AccountManager = AccountManager.get(this)
                val options = Bundle()
                val new = account?.account
                val letHandler = Handler(Looper.getMainLooper())
                letHandler.post(error)

                am.getAuthToken(
                    new,                     // Account retrieved using getAccountsByType()
                    "Manage your tasks",            // Auth scope
                    options,                        // Authenticator-specific options
                    this,                           // Your activity
                    OnTokenAcquired(),              // Callback called when a token is successfully acquired
                    letHandler            // Callback called if an error occurs
                )
                isRunning = true
                // QUA SI USA FIREBASE
                FirebaseFirestore.setLoggingEnabled(true)
                db = FirebaseFirestore.getInstance()
                val docRef = db.collection("user")
                if (id != null) {
                    if (!db.document("user/$id").get().isSuccessful) {
                        docRef.document(id).set({})
                    }
                }
                val intent = Intent(this, NotifyService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startService(intent)
                }
                else{
                    startService(intent)
                }
                if (id != null) {
                    createPoiList(id)
                    createFriendList(id)
                    createLiveList(id)
                    createCarList(id)
                }
                val x = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
                val googleButton = findViewById<Button>(R.id.google_button)
                val imageView = x.findViewById<ImageView>(R.id.imageView)
                val user = x.findViewById<TextView>(R.id.user)
                val email = x.findViewById<TextView>(R.id.email)
                val close = x.findViewById<ImageView>(R.id.close)
                val autoCompleteFragment =
                    supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment
                val layout: LinearLayout = autoCompleteFragment?.view as LinearLayout
                val menuIcon: ImageView = layout.getChildAt(0) as ImageView
                googleButton.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                // load google photo
                Picasso.get().load(account?.photoUrl).into(imageView)
                Picasso.get()
                    .load(account?.photoUrl)
                    .transform(CircleTransform())
                    .resize(100, 100)
                    .into(menuIcon)
                // init menu
                menuIcon.setOnClickListener {
                    switchFrame(
                        drawerLayout,
                        listLayout,
                        homeLayout,
                        friendLayout,
                        friendFrame,
                        carLayout,
                        splashLayout,
                        liveLayout,
                        loginLayout
                    )
                }
                user.visibility = View.VISIBLE
                user.text = account?.displayName
                email.visibility = View.VISIBLE
                email.text = account?.email
                close.visibility = View.VISIBLE

            }
            else if (resultCode == 40) {
                println("non loggato")
                val x = findViewById<NavigationView>(id.nav_view).getHeaderView(0)
                val googleButton = x.findViewById<Button>(id.google_button)
                val close = x.findViewById<ImageView>(R.id.close)
                val user = x.findViewById<TextView>(R.id.user)
                val email = x.findViewById<TextView>(R.id.email)
                val imageView = x.findViewById<ImageView>(R.id.imageView)
                googleButton.visibility = View.VISIBLE
                close.visibility = View.GONE
                imageView.visibility = View.GONE
                user.visibility = View.GONE
                email.visibility = View.GONE
            }
        }
    }
    @SuppressLint("RestrictedApi")
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            id.list -> {
                showPOI()
                return true
            }
            id.help ->{
                Toast.makeText(applicationContext, "help da implementare", Toast.LENGTH_LONG).show()
                return true
            }
            id.friend ->{
                showFriend()
                return true
            }
            id.car ->{
                showCar()
                return true
            }
            id.live ->{
                showLive()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    /*End Override Function*/

    /*Start Utils Function*/

    // init autoComplete fragment to search address
    @SuppressLint("ResourceType")
    fun setUpSearch() {
        val autoCompleteFragment =
            supportFragmentManager.findFragmentById(id.autocomplete_fragment) as? AutocompleteSupportFragment
        autoCompleteFragment?.setCountry("IT")
        autoCompleteFragment?.setPlaceFields(listOf(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS,Place.Field.PHONE_NUMBER,Place.Field.WEBSITE_URI))
        val navMenu: NavigationView = findViewById(id.nav_view)
        navMenu.setNavigationItemSelectedListener(this)
        autoCompleteFragment?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val lat = place.latLng
                if (lat != null) {
                    autoCompleteFragment.setText("")
                    supportFragmentManager.popBackStack()
                    onMapClick(lat)
                }
            }
            override fun onError(status: Status) {
                Log.d("HOY", "An error occurred: ${status.statusMessage}")
            }
        })
    }

    // show menu or home and reDraw all poi
    fun closeDrawer(view: View) {
        println(view)
        if(drawerLayout.visibility == View.GONE) switchFrame(drawerLayout,homeLayout,listLayout,splashLayout,friendLayout,friendFrame,carLayout,liveLayout,loginLayout)
        else {
            reDraw()
            switchFrame(homeLayout,drawerLayout,listLayout,splashLayout,friendLayout,friendFrame,carLayout,liveLayout,loginLayout)
        }
    }

    // populate ListView with poi list
    @SuppressLint("WrongViewCast")
    fun showPOI(){
        var index = 0
        val txt: TextView = findViewById(id.nosrc)
        switchFrame(listLayout,homeLayout,drawerLayout,friendLayout,friendFrame,carLayout,splashLayout,liveLayout,loginLayout)

        val lv:ListView = findViewById(R.id.lv)
        var len = 0
        for (i in myList.keys()){
            if(myList.getJSONObject(i).get("cont") as String != "Macchina" && myList.getJSONObject(i).get("cont") as String != "Live"){
                len++
            }
        }
        val userList = MutableList(len) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in myList.keys()){
            if(myList.getJSONObject(i).get("cont") as String != "Macchina" && myList.getJSONObject(i).get("cont") as String != "Live"){
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
                for (i in myList.keys()){

                    if(selectedItem == myList.getJSONObject(i).get("name") as String) {

                        val mark = mymarker[i] as Marker
                        val removed = myList.getJSONObject(i)
                        mark.remove()
                        mymarker.remove(i)
                        myList.remove(i)
                        val cancel = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = account?.email?.replace("@gmail.com","")
                        // create a Toast to undo the operation of removing
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(cancel) {

                                id?.let { it1 ->
                                    myList.put(mark.position.toString(), removed)
                                    mymarker.put(mark.position.toString(), mark)
                                    writeNewPOI(
                                        it1,
                                        removed.get("name").toString(),
                                        removed.get("addr").toString(),
                                        removed.get("cont").toString(),
                                        removed.get("type").toString(),
                                        mark,
                                        "da implementare",
                                        "da implementare"
                                    )
                                    Toast.makeText(
                                        this,
                                        "undo$selectedItem",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    showPOI()
                                }
                            }
                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()


                        snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                            }
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                //remove from db the poi
                                id?.let { it1 -> db.collection("user").document(it1).collection("marker").get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {
                                            val name = document.data["name"]
                                            if(name == selectedItem)  {
                                                db.document("user/"+id+"/marker/"+document.id).delete()
                                                showPOI()
                                                return@addOnSuccessListener
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.d("FAIL", "Error getting documents: ", exception)
                                    }
                                }
                            }
                        })
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
        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val selectedItem = parent.getItemAtPosition(position) as String
            for (i in myList.keys()){
                if(selectedItem == myList.getJSONObject(i).get("name") as String) onMarkerClick(
                    mymarker[i] as Marker)
            }
        }
        lv.adapter = arrayAdapter
    }

    // populate ListView with live list
    fun showLive(){
        val len = myLive.length()
        var index = 0
        val txt: TextView = findViewById(id.nolive)
        switchFrame(liveLayout,listLayout,homeLayout,drawerLayout,friendLayout,friendFrame,carLayout,splashLayout,loginLayout)

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
                if(selectedItem == myLive.getJSONObject(i).get("name") as String) onMarkerClick(
                    mymarker[i] as Marker)
            }
        }
        lv.adapter = arrayAdapter
    }

    // populate ListView with friend list
    @SuppressLint("ShowToast")
    fun showFriend(){
        val len = friendJson.length()
        var index = 0
        val txt: TextView = findViewById(id.nofriend)
        switchFrame(friendLayout,listLayout,homeLayout,drawerLayout,friendFrame,splashLayout,carLayout,liveLayout,loginLayout)


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
                        val id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            snackbar.setAction(cancel) {
                                // Toast to undo operation
                                id?.let { _ -> //it1
                                    friendJson.put(i, selectedItem)
                                    confirmFriend(id, selectedItem)
                                    Toast.makeText(this, "undo $selectedItem", Toast.LENGTH_LONG)
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
                                    removeFriend(id, selectedItem)
                                }
                            }
                        })
                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        showFriend()
                        // remove item from db
                        /* id?.let { it1 -> db.collection("user").document(it1).collection("friend").get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val name = document.data["friend"]
                                    if (name == selectedItem) {
                                        db.document("user/" + id + "/friend/" + document.id)
                                            .delete()
                                        removeFriend(id, selectedItem)
                                        showFriend()
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("FAIL", "Error getting documents: ", exception)
                            }
                        }*/
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


            val url = URL("https://"+ ip + port +"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(selectedItem, "UTF-8"))
            var result: JSONObject
            val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            val alertDialog2 = dialogBuilder.create()

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    println("something went wrong get poi from friend")
                    println(e)
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    this@MapsActivity.runOnUiThread {
                        // retrieve results in a thread to read more time
                        try {
                            alertDialog2.show()
                            result = JSONObject(response.body()?.string()!!)
                            val length = result.length()
                            val markerList = MutableList(length + 1) { "" }
                            var indexMarkerMap = 1
                            // add this empty item cause a bug with spinner, on init select the first item and trigger the onItemSelected
                            markerList[0] = ""
                            for (i in result.keys()) {
                                if (result.getJSONObject(i).get("type") as String == "Pubblico") {
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
                                                    lat = result.getJSONObject(i).get("lat")
                                                        .toString()
                                                        .toDouble()
                                                    lon = result.getJSONObject(i).get("lon")
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
                                                friendLayout,
                                                listLayout,
                                                drawerLayout,
                                                friendFrame,
                                                splashLayout,
                                                carLayout,
                                                liveLayout,
                                                loginLayout
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
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        }
        lv.adapter = arrayAdapter
    }

    // populate ListView with car list
    @SuppressLint("ShowToast")
    fun showCar(){
        val len = myCar.length()
        var index = 0
        var indexFull = 0
        val txt: TextView = findViewById(id.nocar)
        switchFrame(carLayout,friendLayout,listLayout,homeLayout,drawerLayout,friendFrame,splashLayout,liveLayout,loginLayout)


        val  lv: ListView = findViewById(id.lvCar)
        val carList = MutableList(len) { "" }
        val carListFull = MutableList(len*10) { "" }
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in myCar.keys()){
            carList[index] = myCar.getJSONObject(i).get("name") as String
            index++
            for (x in myCar.getJSONObject(i).keys()) {
                carListFull[indexFull] = myCar.getJSONObject(i).get("name") as String
                indexFull++
            }
        }
        val  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, carList)

        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val selectedItem = parent.getItemAtPosition(position) as String
                for(i in myCar.keys()){
                    if(selectedItem == myCar.getJSONObject(i).get("name") as String) {
                        val removed = myCar.getJSONObject(i)
                        val mark = mymarker[i] as Marker
                        mark.remove()
                        myCar.remove(i)
                        mymarker.remove(i)
                        val cancel = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(cancel) {
                                // Toast to undo remove operation
                                id?.let { _ -> //it1
                                    myCar.put(i, removed)
                                    mymarker.put(i, mark)
                                    Toast.makeText(this, "undo$selectedItem", Toast.LENGTH_LONG)
                                    showCar()
                                }
                            }

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
                        snackbar.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                            }

                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                id?.let { it1 -> db.collection("user").document(it1).collection("marker").get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {
                                            val name = document.data["name"]
                                            if(name == selectedItem)  {
                                                db.document("user/"+id+"/marker/"+document.id).delete()
                                                return@addOnSuccessListener
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.d("FAIL", "Error getting documents: ", exception)
                                    }
                                }
                                //remove from db
                                id?.let { it1 -> db.collection("user").document(it1).collection("car").get()
                                    .addOnSuccessListener { result ->
                                        for (document in result) {
                                            val name = document.data["name"]
                                            if(name == selectedItem)  {
                                                db.document("user/"+id+"/car/"+document.id).delete()
                                                showCar()
                                                return@addOnSuccessListener
                                            }
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.d("FAIL", "Error getting documents: ", exception)
                                    }
                                }
                            }
                        })
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

        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_car_view, null)
            val txtName :TextView = dialogView.findViewById(id.car_name_txt)
            val address : TextView = dialogView.findViewById(id.carAddressValue)
            val timer : TimePicker = dialogView.findViewById(id.timePickerView)
            val remindButton : Button = dialogView.findViewById(R.id.remindButton)
            var key = ""
            val selectedItem = parent.getItemAtPosition(position) as String

            txtName.text = selectedItem
            for (i in myCar.keys()){
                if(myCar.getJSONObject(i).get("name") as String == selectedItem){
                    key = i
                    address.text = myCar.getJSONObject(i).get("addr") as String
                    val time = (myCar.getJSONObject(i).get("timer").toString()).toInt()
                    val hour = time/60
                    val minute = time - hour*60
                    timer.setIs24HourView(true)
                    timer.hour = hour
                    timer.minute = minute
                }
            }

            remindButton.setOnClickListener {
                myCar.getJSONObject(key).put("timer",timer.hour*60 + timer.minute)
                alertDialog.dismiss()
                resetTimerAuto(myCar.getJSONObject(key))
            }

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener { }
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
        lv.adapter = arrayAdapter
    }

    //get email inserted to send a request via server
    fun addFriend(view: View) {
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
                account?.email?.replace("@gmail.com","")?.let { it1 -> sendFriendRequest(emailText.text.toString(),it1) }
                alertDialog.dismiss()
            }
        }
    }

    /*End Utils Function*/

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation === Configuration.ORIENTATION_LANDSCAPE) {

            onSaveInstanceState(newBundy)
        } else if (newConfig.orientation === Configuration.ORIENTATION_PORTRAIT) {

            onSaveInstanceState(newBundy)
        }
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



