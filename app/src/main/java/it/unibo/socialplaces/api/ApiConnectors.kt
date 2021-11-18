package it.unibo.socialplaces.api

import it.unibo.socialplaces.config.Api
import it.unibo.socialplaces.config.Auth
import okhttp3.ResponseBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*

object ApiConnectors {
    // SSL certificate configuration
    private val trustStore: KeyStore = KeyStore.getInstance("BKS")
    private const val keyPair = "SistemiContextAware2021@*"

    fun loadStore(rawStore: InputStream) {
        trustStore.load(rawStore, keyPair.toCharArray())
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

    /**
     * OkHttp client to be used inside a Retrofit builder.
     * @see retrofitBuilder
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader("Authorization", "Bearer " + Auth.getToken())
                    .build()
                chain.proceed(request)
            }).build()
    }

    /**
     * Retrofit base client builder for all the interfaces in the package "api".
     * @see friendsApi
     * @see liveEventsApi
     * @see notificationApi
     * @see pointsOfInterestApi
     * @see recommendationApi
     */
    private val retrofitBuilder: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Retrofit library error converter.
     */
    private val errorConverter: Converter<ResponseBody, ApiError> by lazy {
        retrofitBuilder.responseBodyConverter(ApiError.Message::class.java, emptyArray<Annotation>())
    }

    /**
     * @see FriendsApi
     */
    val friendsApi: FriendsApi by lazy {
        retrofitBuilder.create(FriendsApi::class.java)
    }

    /**
     * @see LiveEventsApi
     */
    val liveEventsApi: LiveEventsApi by lazy {
        retrofitBuilder.create(LiveEventsApi::class.java)
    }

    /**
     * @see PointsOfInterestApi
     */
    val pointsOfInterestApi: PointsOfInterestApi by lazy {
        retrofitBuilder.create(PointsOfInterestApi::class.java)
    }

    /**
     * @see NotificationApi
     */
    val notificationApi: NotificationApi by lazy {
        retrofitBuilder.create(NotificationApi::class.java)
    }

    /**
     * @see RecommendationApi
     */
    val recommendationApi: RecommendationApi by lazy {
        retrofitBuilder.create(RecommendationApi::class.java)
    }

    /**
     * Given an HTTP Response body (handled by the Retrofit library), returns an instance
     * of [ApiError].
     *
     * @param body of type [ResponseBody]
     * @return instance of type [ApiError.Generic] if [body] is `null`, an instance of [ApiError.Message]
     * otherwise.
     */
    fun handleApiError(body: ResponseBody?): ApiError {
        if(body == null) {
            return ApiError.Generic()
        }
        return errorConverter.convert(body) ?: ApiError.Generic()
    }
}