package it.unibo.socialplaces.security

import android.content.Context
import android.util.Base64
import it.unibo.socialplaces.R
import java.security.spec.X509EncodedKeySpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyGenParameterSpec
import android.util.Log
import java.math.BigInteger
import java.security.*
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal


object RSA {
    private val TAG = RSA::class.qualifiedName!!

    private const val KEY_ALIAS = "social_places_client"

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    private lateinit var serverPublicKey: PublicKey
    private lateinit var privateKey: PrivateKey
    private lateinit var publicKey: PublicKey

    /**
     * The public key with which the server can encode messages.
     */
    val devicePublicKey get() = "-----BEGIN PUBLIC KEY-----\n" +
            String(Base64.encode(publicKey.encoded, Base64.NO_WRAP)) +
            "\n-----END PUBLIC KEY-----"

    fun loadServerPublicKey(context: Context) {
        Log.v(TAG, "loadServerPublicKey")

        try {
            with(context.resources.openRawResource(R.raw.public_key)) {
                val keySpec = X509EncodedKeySpec(readBytes())
                val keyFactory = KeyFactory.getInstance("RSA")
                serverPublicKey = keyFactory.generatePublic(keySpec)
            }
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it)}
        }
    }

    /**
     * Loads the public and private keys stored in the Android KeyStore.
     * @return `true` if the keys got loaded, `false` if not.
     */
    fun loadDeviceKeys(): Boolean {
        Log.v(TAG, "loadDeviceKeys")
        val ks: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
            load(null)
        }

        return try {
            val ksEntry = ks.getEntry(KEY_ALIAS, null)

            if (ksEntry is KeyStore.PrivateKeyEntry) {
                privateKey = ksEntry.privateKey
                publicKey = ksEntry.certificate.publicKey
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, "Error loading the keys for the device.\n$it") }
            false
        }
    }

    /**
     * Generates a new pair of RSA public/private keys and stores them in the Android KeyStore.
     */
    fun generateDeviceKeys() {
        Log.v(TAG, "generateDeviceKeys")
        // We are creating a RSA key pair and store it in the Android Keystore
        val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE)

        // We are creating the key pair with sign and verify purposes
        val parameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setCertificateSerialNumber(BigInteger.valueOf(777)) // Serial number used for the self-signed certificate of the generated key pair, default is 1
            setCertificateSubject(X500Principal("CN=$KEY_ALIAS")) // Subject used for the self-signed certificate of the generated key pair, default is CN=fake
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1, KeyProperties.ENCRYPTION_PADDING_RSA_OAEP) // Set of padding schemes with which the key can be used when signing/verifying
            setKeySize(4096)
            // TODO Enable only if the application is used on a real device.
//            setUserAuthenticationRequired(true) // Sets whether this key is authorized to be used only if the user has been authenticated, default false
//            setUserAuthenticationParameters(30, KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG) //Duration(seconds) for which this key is authorized to be used after the user is successfully authenticated
            build()
        }

        // Initialization of key generator with the parameters we have specified above
        keyPairGenerator.initialize(parameterSpec)

        // Generates the key pair and stores them in the Android KeyStore
        val keyPair = keyPairGenerator.generateKeyPair()

        privateKey = keyPair.private
        publicKey = keyPair.public
    }

    fun encrypt(plaintext: String): String {
        Log.v(TAG, "encrypt")
        return try {
            val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey)
            val encrypted = cipher.doFinal(plaintext.encodeToByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it) }
            ""
        }
    }

    fun decrypt(ciphertext: String): String {
        Log.v(TAG, "decrypt")
        return try {
            val cipher: Cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            val decrypted = cipher.doFinal(Base64.decode(ciphertext, Base64.DEFAULT))
            String(decrypted)
        } catch (e: Exception) {
            e.message?.let { Log.e(TAG, it) }
            ""
        }
    }
}