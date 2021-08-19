package com.example.maptry.server

import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.mapsActivityContext
import com.example.maptry.config.Api
import okhttp3.*
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

// SSL certificate configuration
val trustStore: KeyStore = KeyStore.getInstance("BKS")
const val keyPair = "SistemiContextAware2021@*"

val res = mapsActivityContext.resources.openRawResource(R.raw.mystore).use {
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
    hv.verify(Api.ip, session)
    return@HostnameVerifier true
}
