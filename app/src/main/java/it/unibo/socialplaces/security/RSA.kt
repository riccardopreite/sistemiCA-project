package it.unibo.socialplaces.security

import android.content.Context
import android.util.Base64
import it.unibo.socialplaces.R
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.security.spec.X509EncodedKeySpec
import android.security.keystore.KeyProperties

import android.security.keystore.KeyGenParameterSpec
import android.util.Log
import it.unibo.socialplaces.api.ApiConnectors
import java.math.BigInteger
import java.security.*
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal


object RSA {
    private val TAG = RSA::class.qualifiedName!!

    private val mediaType = "text/plain; charset=utf-8".toMediaTypeOrNull()!!

    private const val KEY_ALIAS = "social_places_client"

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    private lateinit var _serverPublicKey: PublicKey
    private lateinit var _devicePrivateKey: PrivateKey
    private lateinit var _devicePublicKey: PublicKey

    /**
     * The public key with which the server can encode messages.
     */
    val devicePublicKey get() = String(Base64.encode(_devicePublicKey.encoded, Base64.DEFAULT))

    fun loadServerPublicKey(context: Context) {
        Log.v(TAG, "loadServerPublicKey")
        val strPubKey = context.getString(R.string.social_places_api_public_key)
        try {
            val publicBytes: ByteArray = Base64.decode(strPubKey, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(publicBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            _serverPublicKey = keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads the public and private keys stored in the Android KeyStore.
     * @return `true` if the keys get loaded, `false` if not.
     */
    fun loadDeviceKeys(): Boolean {
        Log.v(TAG, "loadDeviceKeys")
        val ks: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        return try {
            val ksEntry = ks.getEntry(KEY_ALIAS, null)
            if(ksEntry is KeyStore.PrivateKeyEntry) {
                _devicePrivateKey = ksEntry.privateKey
                _devicePublicKey = ksEntry.certificate.publicKey
                Log.i(TAG, "PUBLIC: $_devicePublicKey")
                Log.i(TAG, "PRIVATE: $_devicePrivateKey")
            } else {
                Log.i(TAG, ksEntry.toString())
            }
            true
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, "Error loading the keys for the device.\n$it") }
            false
        }
    }

    /**
     * Generates a new pair of RSA public/private keys and stores them in the Android KeyStore.
     */
    fun generateDeviceKeys() {
        // We are creating a RSA key pair and store it in the Android Keystore
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)

        // We are creating the key pair with sign and verify purposes
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setCertificateSerialNumber(BigInteger.valueOf(777)) // Serial number used for the self-signed certificate of the generated key pair, default is 1
            setCertificateSubject(X500Principal("CN=$KEY_ALIAS")) // Subject used for the self-signed certificate of the generated key pair, default is CN=fake
            setDigests(KeyProperties.DIGEST_SHA256) // Set of digest algorithms with which the key can be used
            setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1, KeyProperties.SIGNATURE_PADDING_RSA_PSS) // Set of padding schemes with which the key can be used when signing/verifying
            setUserAuthenticationRequired(true) // Sets whether this key is authorized to be used only if the user has been authenticated, default false
            setUserAuthenticationParameters(30, KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG) //Duration(seconds) for which this key is authorized to be used after the user is successfully authenticated
            build()
        }

        // Initialization of key generator with the parameters we have specified above
        keyPairGenerator.initialize(parameterSpec)

        // Generates the key pair
        val keyPair = keyPairGenerator.generateKeyPair()

        _devicePrivateKey = keyPair.private
        _devicePublicKey = keyPair.public
    }

    fun encrypt(plaintext: String): String {
        return try {
            val cipher: Cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.ENCRYPT_MODE, _serverPublicKey)
            val encrypted = cipher.doFinal(plaintext.encodeToByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun decrypt(ciphertext: String): String {
        return try {
            val cipher: Cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.DECRYPT_MODE, _devicePrivateKey)
            val decrypted = cipher.doFinal(Base64.decode(ciphertext, Base64.DEFAULT))
            String(decrypted)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ""
        }
    }
}