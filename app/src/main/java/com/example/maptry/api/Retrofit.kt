package com.example.maptry.api

import com.example.maptry.R
import com.example.maptry.activity.MapsActivity
import com.example.maptry.config.Api
import com.example.maptry.config.Auth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

object Retrofit {

    // SSL certificate configuration
    private val trustStore: KeyStore = KeyStore.getInstance("BKS")
    private const val keyPair = "SistemiContextAware2021@*"

    val res = MapsActivity.mapsActivityContext.resources.openRawResource(R.raw.mystore).use {
        trustStore.load(it,keyPair.toCharArray())
    }

    private val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(trustStore)
    }
    private val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
        init(null, tmf.trustManagers, SecureRandom())
    }
    private val trustManager = tmf.trustManagers[0] as X509TrustManager

    private val hostnameVerifier = HostnameVerifier { _, session -> //first
        val hv: HostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
        hv.verify(Api.ip, session)
        return@HostnameVerifier true
    }

    private val client: OkHttpClient by lazy { // TODO Sicuri che basti creare un'istanza unica?
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + Auth.userToken)
                    .build()

                chain.proceed(request)
            }).build()
    }

    private val retrofitBuilder: Retrofit by lazy { // TODO Sicuri che basti creare un'istanza unica?
        Retrofit.Builder()
            .baseUrl("https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val friendsApi: FriendsApi by lazy {
        retrofitBuilder
            .create(FriendsApi::class.java)
    }

    val liveEventsApi: LiveEventsApi by lazy {
        retrofitBuilder
            .create(LiveEventsApi::class.java)
    }

    val pointOfInterestsApi: PointOfInterestsApi by lazy {
        retrofitBuilder
            .create(PointOfInterestsApi::class.java)
    }
}