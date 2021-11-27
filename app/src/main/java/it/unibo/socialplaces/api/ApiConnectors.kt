package it.unibo.socialplaces.api

import it.unibo.socialplaces.config.Api
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.security.RSA
import okhttp3.ResponseBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.*
import javax.net.ssl.*

object ApiConnectors {
//    private val TAG = ApiConnectors::class.qualifiedName!!

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
     * OkHttp secure client to be used inside a Retrofit builder.
     * @see secureRetrofitBuilder
     */
    private val secureClient: OkHttpClient by lazy {
        val mediaType = "text/plain; charset=utf-8".toMediaTypeOrNull()

        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request()
                val oldBody = request.body
                val buffer = Buffer()
                oldBody?.writeTo(buffer)
                val strOldBody = buffer.readUtf8()
                val strNewBody: String = RSA.encrypt(strOldBody)
                val body: RequestBody = strNewBody.toRequestBody(mediaType)

                val newRequest = request
                    .newBuilder()
                    .header("Content-Type", body.contentType().toString())
                    .header("Content-Length", body.contentLength().toString())
                    .method(request.method, body)
                    .addHeader("Authorization", "Bearer " + Auth.getToken())
                    .build()

                chain.proceed(newRequest)
            }).build()
    }

    /**
     * Retrofit base client builder for all the interfaces in the package "api".
     * @see friendsApi
     * @see liveEventsApi
     * @see notificationApi
     * @see pointsOfInterestApi
     */
    private val retrofitBuilder: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Retrofit base client builder for all the interfaces in the package "api".
     * @see recommendationApi
     */
    private val secureRetrofitBuilder: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}")
            .client(secureClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Retrofit library error converter.
     */
    private val errorConverter: Converter<ResponseBody, ApiError> by lazy {
        retrofitBuilder.responseBodyConverter(ApiError::class.java, emptyArray<Annotation>())
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
        secureRetrofitBuilder.create(RecommendationApi::class.java)
    }

    /**
     * Given an HTTP Response body (handled by the Retrofit library), returns an instance
     * of [ApiError].
     *
     * @param body of type [ResponseBody]
     * @return instance of type [ApiError]
     * otherwise.
     */
    fun handleApiError(body: ResponseBody?): ApiError {
        if(body == null) {
            return ApiError()
        }
        return errorConverter.convert(body) ?: ApiError()
    }
}