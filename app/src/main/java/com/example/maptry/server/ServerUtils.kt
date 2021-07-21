package com.example.maptry.server

import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*


// SSL certificate configuration
val trustStore: KeyStore = KeyStore.getInstance("BKS")
const val keyPair = "SistemiContextAware2021@*"

val res = context.resources.openRawResource(R.raw.mystore).use {
    trustStore.load(it,keyPair.toCharArray())
}
val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
    init(trustStore)
}
val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
    init(null, tmf.trustManagers, SecureRandom())
}
val trustManager = tmf.trustManagers[0] as X509TrustManager


val hostnameVerifier = HostnameVerifier { _, session -> //first
    val hv: HostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
    hv.verify(ip, session)
    return@HostnameVerifier true
}


fun checkUser(id: String){
    val url = URL("https://$ip$port/") //Create this POST api to check if user already exist in DB, if not create it

    val formBody: RequestBody = FormBody.Builder()
        .add("username", id)
        .build()

    val request: Request = Request.Builder()
        .url(url)
        .post(formBody)
        .build()

    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("Check user is success")
    }
    else{
        println("Check user is error")
        println(response.message())
    }
}
/*Start Server Function*/


fun startLive(live:JSONObject){
    val url = URL("https://"+ip+port+"/startLive?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(live.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(live.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(live.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(live.get("address") as String, "UTF-8"))
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong start live")
            println(e)

        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.string())
        }
    })
}

/*End Server Function*/