package com.example.maptry.activity

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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
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
import java.util.Arrays.asList

@Suppress("DEPRECATION", "DEPRECATED_IDENTITY_EQUALS",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class MapsActivity  : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener,NavigationView.OnNavigationItemSelectedListener{

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationUpdateState = false
    private var timer = Timer()
    private var mainHandler = Handler()

    private var run = object : Runnable {
        override fun run() {
            if(drawed) { // wait firebase to load JSON or 1.5 sec
                val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
                val listLayout: FrameLayout = findViewById(R.id.list_layout)
                val homeLayout: FrameLayout = findViewById(R.id.homeframe)
                val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
                val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
                val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
                val carLayout: FrameLayout = findViewById(R.id.car_layout)
                val liveLayout: FrameLayout = findViewById(R.id.live_layout)
                val loginLayout: FrameLayout = findViewById(R.id.login_layout)
                switchFrame(homeLayout,listLayout,drawerLayout,friendLayout,friendRequestLayout,splashLayout,carLayout,liveLayout,loginLayout)
                mainHandler.removeCallbacksAndMessages(null);
            }
            else {
                mainHandler.postDelayed(this, 1500)
            }
        }
    }

    companion object {
        var builder = LocationSettingsRequest.Builder()
        lateinit var locationCallback: LocationCallback
        var newBundy = Bundle()
        var mLocationRequest: LocationRequest? = null
        private val UPDATE_INTERVAL = (10 * 1000).toLong()  /* 10 secs */
        private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
        val REQUEST_LOCATION_PERMISSION = 1
        var ip = "casadiso.ddns.net"
//        var port = port+"" //oldport
        var port = ""
        var isRunning : Boolean = false
        lateinit var firebaseAuth: FirebaseAuth
        var zoom = 1
        var oldPos :Marker? = null
        lateinit var lastLocation: Location
        @SuppressLint("StaticFieldLeak")
        lateinit var context : Context
        lateinit var alertDialog: AlertDialog
        lateinit var mMap: GoogleMap
        var addrThread:Thread? = null
        lateinit var geocoder : Geocoder
        var listAddr:MutableList<Address>? = null
        var drawed = false
        var myjson = JSONObject() //tmp json
        var mymarker = JSONObject() //marker
        val myList = JSONObject() // POI json
        val myCar = JSONObject() // car json
        val myLive = JSONObject() // live json
        lateinit var mAnimation : Animation
        lateinit var dataFromfirebase: DataSnapshot
        var account : GoogleSignInAccount? = null
        lateinit var dataFromfirestore :List<DocumentSnapshot>
        @SuppressLint("StaticFieldLeak")
        lateinit var db :FirebaseFirestore
        private const val REQUEST_CHECK_SETTINGS = 2
        var friendJson = JSONObject() // friend json
        var friendTempPoi = JSONObject()
    }

    /*Start Initialize Function*/
    protected fun startLocationUpdates() {
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
        val mapFragment =  supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        var locationButton : View? = mapFragment.view?.findViewById<LinearLayout>(Integer.parseInt("1"))
        val prov : View? = (locationButton?.parent) as View
        locationButton = prov?.findViewById(Integer.parseInt("2"))
        val layoutParams = locationButton?.getLayoutParams() as RelativeLayout.LayoutParams
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
        layoutParams.setMargins(0, 0, 30, 30)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy")
        isRunning = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(isRunning) {
            return
        }
        setContentView(R.layout.activity_maps)
        isRunning = true
        context = this
        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        geocoder = Geocoder(this)

        //create connection

        val drawerLayout: FrameLayout = findViewById(id.drawer_layout)
        val listLayout: FrameLayout = findViewById(id.list_layout)
        val homeLayout: FrameLayout = findViewById(id.homeframe)
        val splashLayout: FrameLayout = findViewById(id.splashFrame)
        val friendLayout: FrameLayout = findViewById(id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(id.friendFrame)
        val carLayout: FrameLayout = findViewById(id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.enlarge);
        mAnimation.backgroundColor = Color.TRANSPARENT;

        switchFrame(splashLayout,drawerLayout,listLayout,homeLayout,friendLayout,friendRequestLayout,carLayout,liveLayout,loginLayout)


        mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(run)

        // start intent to log in user
        val menuIntent =  Intent(this, LoginActivity::class.java)
        val component = ComponentName(this, LoginActivity::class.java)
        intent.component = component
        startActivityForResult(menuIntent,40);

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
                        when (exception.getStatusCode()) {
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
        if(myList.getJSONObject(p0.position.toString()).get("cont") as String == "Live"){
            phone.text = myLive.getJSONObject(p0.position.toString()).get("timer") as String + " minuti"
            phoneCap.text = "Timer"
            url.text = myLive.getJSONObject(p0.position.toString()).get("owner") as String
            urlCap.text = "Proprietario"

        }
        else if( myList.getJSONObject(p0.position.toString()).get("cont") as String == "Macchina"){
            phone.text = myCar.getJSONObject(p0.position.toString()).get("timer") as String + " minuti"
            phoneCap.text = "Timer"
            urlCap.text = "Proprietario"
            url.text = myCar.getJSONObject(p0.position.toString()).get("owner") as String
        }
        else{
            phoneCap.text = "N.cellulare"
            urlCap.text = "WebSite"
        }

        val routebutton: Button = dialogView.findViewById(id.routeBtn)
        val removebutton: Button = dialogView.findViewById(id.removeBtnattr)
        removebutton.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type="text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, "https://maps.google.com/?q="+ myList.getJSONObject(p0.position.toString()).get("lat")+","+ myList.getJSONObject(p0.position.toString()).get("lon"));
            startActivity(Intent.createChooser(shareIntent,"Stai condividendo "+ myList.getJSONObject(p0.position.toString()).get("name")))
            alertDialog.dismiss()
        }
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
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
                switchFrame(homeLayout,listLayout,drawerLayout,friendLayout,friendRequestLayout,carLayout,splashLayout,liveLayout,loginLayout)
                alertDialog.dismiss()
            }
        }
        else{
            routebutton.setOnClickListener {
                var intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + address.text))
                startActivity(intent)
                alertDialog.dismiss()
            }
        }

        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
            override fun onDismiss(arg0: DialogInterface) { }
        })
        dialogBuilder.setView(dialogView)

        alertDialog = dialogBuilder.create();
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
        val timePickerLayout = dialogView.findViewById<RelativeLayout>(R.id.timePicker)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.timePicker1)
        val addbutton: Button = dialogView.findViewById(id.addBtn)
        val removebutton: Button = dialogView.findViewById(id.removeBtn)
        val id = account?.email?.replace("@gmail.com", "")

        timePicker.hour = 3
        timePicker.minute = 0
        val radioGroup = dialogView.findViewById<RelativeLayout>(R.id.rl_gender)
        var time : Int;
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
            var text = lname.text.toString()
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

                if (spinner.selectedItem.toString() == "Macchina") {
                    for (i: String in myCar.keys()) {
                        try {
                            var x: String = myCar.getJSONObject(i).get("name") as String
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
                    var marker = createMarker(p0)
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
                else if(spinner.selectedItem.toString() == "Live"){
                    for (i: String in myLive.keys()) {
                        try {
                            var x: String = myLive.getJSONObject(i).get("name") as String
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
                else{
                    for (i: String in myList.keys()) {
                        try {
                            var x: String = myList.getJSONObject(i).get("name") as String
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
                    var marker = createMarker(p0)
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
                alertDialog.dismiss()
            }
        }
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
            override fun onDismiss(arg0: DialogInterface) {

            }
        })
        dialogBuilder.setView(dialogView)

        alertDialog = dialogBuilder.create();
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
                val place = data?.let { Autocomplete.getPlaceFromIntent(it) };
                Log.i("OK", "Place: " + place?.getName() + ", " + place?.getId());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = data?.let { Autocomplete.getStatusFromIntent(it) };
                if (status != null) {
                    Log.i("errpr", status.getStatusMessage())
                };
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
        else if (requestCode == 40) {
            if (resultCode == 50) {
                println("loggato")
                // user logged, init structure, create user in firebase if not exist
                account = GoogleSignIn.getLastSignedInAccount(this@MapsActivity)
                var id: String? = account?.email?.replace("@gmail.com", "")
                isRunning = true
                FirebaseFirestore.setLoggingEnabled(true)
                db = FirebaseFirestore.getInstance()
                var docRef = db.collection("user")
                if (id != null) {
                    if (!db.document("user/" + id).get().isSuccessful) {
                        docRef.document(id).set({})
                    }
                }
                val intent: Intent = Intent(this, NotifyService::class.java)
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
                val google_button = findViewById<Button>(R.id.google_button)
                val imageView = x.findViewById<ImageView>(R.id.imageView)
                val user = x.findViewById<TextView>(R.id.user)
                val email = x.findViewById<TextView>(R.id.email)
                val close = x.findViewById<ImageView>(R.id.close)
                val autoCompleteFragment =
                    supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as? AutocompleteSupportFragment
                val layout: LinearLayout = autoCompleteFragment?.view as LinearLayout
                val menuIcon: ImageView = layout.getChildAt(0) as ImageView
                google_button.visibility = View.GONE
                imageView.visibility = View.VISIBLE
                // load google photo
                Picasso.get().load(account?.photoUrl).into(imageView)
                Picasso.get()
                    .load(account?.photoUrl)
                    .transform(CircleTransform())
                    .resize(100, 100)
                    .into(menuIcon)
                // init menu
                menuIcon.setOnClickListener(View.OnClickListener() {
                    val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
                    val listLayout: FrameLayout = findViewById(R.id.list_layout)
                    val homeLayout: FrameLayout = findViewById(R.id.homeframe)
                    val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
                    val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
                    val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
                    val carLayout: FrameLayout = findViewById(R.id.car_layout)
                    val liveLayout: FrameLayout = findViewById(R.id.live_layout)
                    val loginLayout: FrameLayout = findViewById(R.id.login_layout)
                    switchFrame(drawerLayout, listLayout, homeLayout,friendLayout,friendRequestLayout,carLayout,splashLayout,liveLayout,loginLayout)
                })
                user.visibility = View.VISIBLE
                user.text = account?.displayName
                email.visibility = View.VISIBLE
                email.text = account?.email
                close.visibility = View.VISIBLE

            }
            else if (resultCode == 40) {
                println("non loggato")
                var x = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
                var google_button = x.findViewById<Button>(R.id.google_button)
                var close = x.findViewById<ImageView>(R.id.close)
                var user = x.findViewById<TextView>(R.id.user)
                var email = x.findViewById<TextView>(R.id.email)
                var imageView = x.findViewById<ImageView>(R.id.imageView)
                google_button.visibility = View.VISIBLE
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
        autoCompleteFragment?.setPlaceFields(asList(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS,Place.Field.PHONE_NUMBER,Place.Field.WEBSITE_URI))
        val navMenu: NavigationView = findViewById(R.id.nav_view)
        navMenu.setNavigationItemSelectedListener(this)
        autoCompleteFragment?.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                var lat = place.latLng
                if (lat != null) {
                    autoCompleteFragment.setText("")
                    supportFragmentManager.popBackStack();
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
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val listFriendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)

        if(drawerLayout.visibility == View.GONE) switchFrame(drawerLayout,homeLayout,listLayout,splashLayout,listFriendLayout,friendLayout,carLayout,liveLayout,loginLayout)
        else {
            reDraw()
            switchFrame(homeLayout,drawerLayout,listLayout,splashLayout,listFriendLayout,friendLayout,carLayout,liveLayout,loginLayout)
        }
    }

    // populate ListView with poi list
    @SuppressLint("WrongViewCast")
    fun showPOI(){
        var index = 0
        val txt: TextView = findViewById(R.id.nosrc)
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(listLayout,homeLayout,drawerLayout,friendLayout,friendRequestLayout,carLayout,splashLayout,liveLayout,loginLayout)

        val lv:ListView = findViewById<ListView>(R.id.lv)
        var len = 0
        for (i in myList.keys()){
            if(myList.getJSONObject(i).get("cont") as String != "Macchina" && myList.getJSONObject(i).get("cont") as String != "Live"){
                len++
            }
        }
        val userList = MutableList<String>(len,{""})
        if(len == 0) txt.visibility = View.VISIBLE;
        else txt.visibility = View.INVISIBLE
        for (i in myList.keys()){
            if(myList.getJSONObject(i).get("cont") as String != "Macchina" && myList.getJSONObject(i).get("cont") as String != "Live"){
                userList[index] = myList.getJSONObject(i).get("name") as String
                index++
            }
        }
        val arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList);

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
                        val AC:String
                        AC = "Annulla"
                        val text = "Rimosso "+selectedItem.toString()
                        val id = account?.email?.replace("@gmail.com","")
                        // create a Toast to undo the operation of removing
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(AC,View.OnClickListener {

                                id?.let { it1 ->
                                    myList.put(mark.position.toString(),removed)
                                    mymarker.put(mark.position.toString(),mark)
                                    writeNewPOI(it1,removed.get("name").toString(),removed.get("addr").toString(),removed.get("cont").toString(),removed.get("type").toString(),mark,"da implementare","da implementare")
                                    Toast.makeText(this,"undo" + selectedItem.toString(),Toast.LENGTH_LONG).show()

                                    showPOI()
                                }
                            })
                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()

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
                        alertDialog.dismiss()
                        break
                    }
                }

            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create();
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
        lv.adapter = arrayAdapter;
    }

    // populate ListView with live list
    fun showLive(){
        val len = myLive.length()
        var index = 0
        val txt: TextView = findViewById(R.id.nolive)
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)

        switchFrame(liveLayout,listLayout,homeLayout,drawerLayout,friendLayout,friendRequestLayout,carLayout,splashLayout,loginLayout)

        var  lv:ListView = findViewById<ListView>(R.id.lvLive)
        val userList = MutableList<String>(len,{""})
        if(len == 0) txt.visibility = View.VISIBLE;
        else txt.visibility = View.INVISIBLE;
        for (i in myLive.keys()){
            userList[index] = myLive.getJSONObject(i).get("name") as String
            index++
        }


        var  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userList);

        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val selectedItem = parent.getItemAtPosition(position) as String
            for (i in myLive.keys()){
                if(selectedItem == myLive.getJSONObject(i).get("name") as String) onMarkerClick(
                    mymarker[i] as Marker)
            }
        }
        lv.adapter = arrayAdapter;
    }

    // populate ListView with friend list
    @SuppressLint("ShowToast")
    private fun showFriend(){
        val len = friendJson.length()
        var index = 0
        val txt: TextView = findViewById(R.id.nofriend)
        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(friendLayout,listLayout,homeLayout,drawerLayout,friendRequestLayout,splashLayout,carLayout,liveLayout,loginLayout)


        var  lv:ListView = findViewById<ListView>(R.id.fv)
        val friendList = MutableList<String>(len,{""})
        if(len == 0) txt.visibility = View.VISIBLE
        else txt.visibility = View.INVISIBLE
        for (i in friendJson.keys()){
            friendList[index] = friendJson[i] as String
            index++
        }

        var  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, friendList)
        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val selectedItem = parent.getItemAtPosition(position) as String

                for(i in friendJson.keys()){
                    if(selectedItem == friendJson[i] as String) {
                        val removed = selectedItem
                        friendJson.remove(i)
                        val key = i
                        val AC:String
                        AC = "Annulla"
                        val text = "Rimosso $selectedItem"
                        val id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(AC,View.OnClickListener {
                                // Toast to undo operation
                                id?.let { _ -> //it1
                                    friendJson.put(key,removed)
                                    confirmFriend(id,removed)
                                    Toast.makeText(this, "undo$selectedItem",Toast.LENGTH_LONG)
                                    showFriend()

                                }
                            })

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()

                        // remove item from db
                        id?.let { it1 -> db.collection("user").document(it1).collection("friend").get()
                            .addOnSuccessListener { result ->
                                for (document in result) {
                                    val name = document.data["friend"]
                                    if(name == removed)  {
                                        db.document("user/"+id+"/friend/"+document.id).delete()
                                        removeFriend(id,removed)
                                        showFriend()
                                        return@addOnSuccessListener
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.d("FAIL", "Error getting documents: ", exception)
                            }
                        }
                        alertDialog.dismiss()
                        break
                    }
                }

            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create();
            alertDialog.show()
            return@setOnItemLongClickListener true
        }

        lv.setOnItemClickListener { parent, _, position, _ -> //wiew e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_friend_view, null)
            val txtName :TextView = dialogView.findViewById(R.id.friendNameTxt)
            val spinner :Spinner = dialogView.findViewById(R.id.planets_spinner_POI)
            val selectedItem = parent.getItemAtPosition(position) as String

            val context = this
            txtName.text = selectedItem
            // ask public friend's poi with a server call
            val url = URL("https://"+ ip + port +"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(selectedItem, "UTF-8"))
            var result: JSONObject
            val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
            dialogBuilder.setOnDismissListener { }
//            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
//                override fun onDismiss(arg0: DialogInterface) { }
//            })
            dialogBuilder.setView(dialogView)

            var alertDialog2 = dialogBuilder.create();

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    println("something went wrong get poi from friend")
                    println(e)
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    this@MapsActivity.runOnUiThread(Runnable {
                        // retrieve results in a thread to read more time
                        try {
                            alertDialog2.show()
                            result = JSONObject(response.body()?.string()!!)
                            val length = result.length()
                            val markerList = MutableList<String>(length+1,{""})
                            var indexMarkerMap = 1
                            // add this empty item cause a bug with spinner, on init select the first item and trigger the onItemSelected
                            markerList[0] = ""
                            for(i in result.keys()){
                                if(result.getJSONObject(i).get("type") as String == "Pubblico") {
                                    markerList[indexMarkerMap] = result.getJSONObject(i).get("name") as String
                                    indexMarkerMap++
                                }
                            }
                            var arrayAdapter2:ArrayAdapter<String> = ArrayAdapter<String>(context,
                                R.layout.support_simple_spinner_dropdown_item,markerList)
                            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                                override fun onNothingSelected(parent: AdapterView<*>?) {}
                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    if(parent?.getItemAtPosition(position) as String != "") {
                                        var key = ""
                                        val selectedMarker =
                                            parent.getItemAtPosition(position) as String
                                        var lat = 0.0
                                        var lon = 0.0
                                        for (i in result.keys()) {
                                            if (result.getJSONObject(i).get("name") == selectedMarker) {
                                                key = i
                                                lat = result.getJSONObject(i).get("lat").toString()
                                                    .toDouble()
                                                lon = result.getJSONObject(i).get("lon").toString()
                                                    .toDouble()
                                            }
                                        }
                                        var pos: LatLng = LatLng(
                                            lat,
                                            lon
                                        )
                                        var mark = createMarker(pos)
                                        friendTempPoi.put(pos.toString(), result.getJSONObject(key))
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat,lon), 20F))
                                        switchFrame(homeLayout,friendLayout,listLayout,drawerLayout,friendRequestLayout,splashLayout,carLayout,liveLayout,loginLayout)
                                        alertDialog2.dismiss()
                                        showPOIPreferences(pos.toString(),inflater,context,mark!!)
                                    }
                                }
                            }
                            spinner.adapter = arrayAdapter2;
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    })
                }
            })
        }
        lv.adapter = arrayAdapter;
    }

    // populate ListView with car list
    private fun showCar(){
        val len = myCar.length()
        var index = 0
        var indexFull = 0
        val txt: TextView = findViewById(R.id.nocar)


        val drawerLayout: FrameLayout = findViewById(R.id.drawer_layout)
        val listLayout: FrameLayout = findViewById(R.id.list_layout)
        val homeLayout: FrameLayout = findViewById(R.id.homeframe)
        val splashLayout: FrameLayout = findViewById(R.id.splashFrame)
        val friendLayout: FrameLayout = findViewById(R.id.friend_layout)
        val friendRequestLayout: FrameLayout = findViewById(R.id.friendFrame)
        val carLayout: FrameLayout = findViewById(R.id.car_layout)
        val liveLayout: FrameLayout = findViewById(R.id.live_layout)
        val loginLayout: FrameLayout = findViewById(R.id.login_layout)
        switchFrame(carLayout,friendLayout,listLayout,homeLayout,drawerLayout,friendRequestLayout,splashLayout,liveLayout,loginLayout)


        var  lv: ListView = findViewById<ListView>(R.id.lvCar)
        val carList = MutableList<String>(len,{""})
        val carListFull = MutableList<String>(len*10,{""})
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
        var  arrayAdapter : ArrayAdapter<String> = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, carList)

        lv.setOnItemLongClickListener { parent, view, position, _ -> //id

            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_custom_eliminate, null)
            val eliminateBtn: Button = dialogView.findViewById(R.id.eliminateBtn)

            eliminateBtn.setOnClickListener {
                val selectedItem = parent.getItemAtPosition(position) as String
                for(i in myCar.keys()){
                    if(selectedItem == myCar.getJSONObject(i).get("name") as String) {
                        var removed = myCar.getJSONObject(i)
                        var mark = mymarker[i] as Marker
                        mark.remove()
                        myCar.remove(i)
                        mymarker.remove(i)
                        var key = i
                        var AC:String
                        AC = "Annulla"
                        var text = "Rimosso "+ selectedItem
                        var id = account?.email?.replace("@gmail.com","")
                        val snackbar = Snackbar.make(view, text, 5000)
                            .setAction(AC,View.OnClickListener {
                                // Toast to undo remove operation
                                id?.let { _ -> //it1
                                    myCar.put(key,removed)
                                    mymarker.put(key,mark)
                                    Toast.makeText(this,"undo" + selectedItem.toString(), Toast.LENGTH_LONG)
                                    showCar()
                                }
                            })

                        snackbar.setActionTextColor(Color.DKGRAY)
                        val snackbarView = snackbar.view
                        snackbarView.setBackgroundColor(Color.BLACK)
                        snackbar.show()
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
                        alertDialog.dismiss()
                        break
                    }
                }
            }
            val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)
            alertDialog = dialogBuilder.create();
            alertDialog.show()
            return@setOnItemLongClickListener true
        }

        lv.setOnItemClickListener { parent, _, position, _ -> //view e id
            val inflater: LayoutInflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.dialog_car_view, null)
            var txtName :TextView = dialogView.findViewById(R.id.car_name_txt)
            var address : TextView = dialogView.findViewById(R.id.carAddressValue)
            var timer : TimePicker = dialogView.findViewById(R.id.timePickerView)
            var remindButton : Button = dialogView.findViewById(R.id.remindButton)
            var key = ""
            val selectedItem = parent.getItemAtPosition(position) as String

            txtName.text = selectedItem
            for (i in myCar.keys()){
                if(myCar.getJSONObject(i).get("name") as String == selectedItem){
                    key = i
                    address.text = myCar.getJSONObject(i).get("addr") as String
                    var time = (myCar.getJSONObject(i).get("timer").toString()).toInt()
                    var hour = time/60
                    var minute = time - hour*60
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
            dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
                override fun onDismiss(arg0: DialogInterface) { }
            })
            dialogBuilder.setView(dialogView)

            alertDialog = dialogBuilder.create();
            alertDialog.show()
        }
        lv.adapter = arrayAdapter;
    }

    //get email inserted to send a request via server
    fun addFriend(view: View) {
        println(view)
        val inflater: LayoutInflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.add_friend, null)
        val emailText : EditText = dialogView.findViewById(R.id.friendEmail)
        val addBtn: Button = dialogView.findViewById(R.id.friendBtn)
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        dialogBuilder.setOnDismissListener(object : DialogInterface.OnDismissListener {
            override fun onDismiss(arg0: DialogInterface) { }
        })
        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create();
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



