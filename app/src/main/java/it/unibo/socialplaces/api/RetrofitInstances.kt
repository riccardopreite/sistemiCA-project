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

object RetrofitInstances {

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

    private val retrofitBuilder: Retrofit by lazy {
        val builder = Retrofit.Builder()
            .baseUrl("https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        builder.responseBodyConverter<ApiError>(ApiError::class.java, emptyArray<Annotation>())
        builder
    }

    private val errorConverter: Converter<ResponseBody, ApiError> by lazy {
        retrofitBuilder.responseBodyConverter(ApiError.Message::class.java, emptyArray<Annotation>())
    }

    val friendsApi: FriendsApi by lazy {
        retrofitBuilder.create(FriendsApi::class.java)
    }

    val liveEventsApi: LiveEventsApi by lazy {
        retrofitBuilder.create(LiveEventsApi::class.java)
    }

    val pointOfInterestsApi: PointOfInterestsApi by lazy {
        retrofitBuilder.create(PointOfInterestsApi::class.java)
    }

    val notificationApi: NotificationApi by lazy {
        retrofitBuilder.create(NotificationApi::class.java)
    }

    val recommendationApi: RecommendationApi by lazy {
        retrofitBuilder.create(RecommendationApi::class.java)
    }

    fun handleApiError(body: ResponseBody?): ApiError {
        if(body == null) {
            return ApiError.Generic()
        }
        return errorConverter.convert(body) ?: ApiError.Generic()
    }
}