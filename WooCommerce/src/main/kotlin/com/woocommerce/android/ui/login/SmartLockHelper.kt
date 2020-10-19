package com.woocommerce.android.ui.login

import android.content.IntentSender
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.CredentialRequest
import com.google.android.gms.auth.api.credentials.CredentialsOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import java.lang.RuntimeException
import java.lang.ref.WeakReference

class SmartLockHelper(activity: FragmentActivity) {
    interface Callback {
        fun onCredentialsRetrieved(credential: Credential)
        fun onCredentialsUnavailable()
    }

    private var callbackActivity: WeakReference<FragmentActivity>? = null
    private var credentialsClient: GoogleApiClient? = null

    init {
        if (activity is OnConnectionFailedListener && activity is ConnectionCallbacks) {
            callbackActivity = WeakReference(activity)
        } else {
            throw RuntimeException("SmartLockHelper constructor needs an activity that implements " +
                "OnConnectionFailedListener and ConnectionCallbacks")
        }
    }

    private fun getActivityAndCheckAvailability():FragmentActivity? {
        val activity = callbackActivity?.get()
        activity?.let {
            val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(it)
            if (status == ConnectionResult.SUCCESS) {
                return it
            }
        }
        return null
    }

    fun initSmartLockForPasswords(): Boolean {
        getActivityAndCheckAvailability()?.let { activity ->
            val options = CredentialsOptions.Builder().forceEnableSaveDialog().build()
            credentialsClient = GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(activity as ConnectionCallbacks)
                .enableAutoManage(activity, activity as OnConnectionFailedListener)
                .addApi(Auth.CREDENTIALS_API, options)
                .build()
            return true
        }
        return false
    }

    fun smartLockAutoFill(callback: Callback) {
        getActivityAndCheckAvailability()?.let {
            credentialsClient?.let {
                if (!it.isConnected) {
                    return
                }

                println("AMANDA-TEST > SmartLockHelper.smartLockAutoFill > requesting...")

                val credentialRequest = CredentialRequest.Builder()
                    .setPasswordLoginSupported(true)
                    .build()
                Auth.CredentialsApi.request(credentialsClient, credentialRequest).setResultCallback { result ->
                    val status = result.status

                    println("AMANDA-TEST > SmartLockHelper.smartLockAutoFill > $status")

                    if (status.isSuccess) {
                        result.credential?.let { callback.onCredentialsRetrieved(it) }
                    } else {
                        if (status.statusCode == CommonStatusCodes.RESOLUTION_REQUIRED) {
                            try {
                                getActivityAndCheckAvailability()?.let { activity ->
                                    // Prompt the user to choose a saved Credential
                                    status.startResolutionForResult(activity, RequestCodes.SMART_LOCK_READ)
                                }
                            } catch (e: IntentSender.SendIntentException) {
                                WooLog.d(T.LOGIN, "SmartLock: Failed to send resolution for credential request")
                                callback.onCredentialsUnavailable()
                            }
                        } else {
                            // User must log in manually
                            WooLog.d(T.LOGIN, "SmartLock: Unsuccessful credential request")
                            callback.onCredentialsUnavailable()
                        }
                    }
                }
            }
        }
    }

    fun saveCredentialsInSmartLock(username: String, password: String, displayName: String, profilePicture: Uri?) {
        if (password.isEmpty() || username.isEmpty()) {
            WooLog.i(T.LOGIN, "Cannot save SmartLock credentials: username or password is empty")
            return
        }

        getActivityAndCheckAvailability()?.let { activity ->
            credentialsClient?.let {
                if (!it.isConnected) {
                    println("AMANDA-TEST > SmartLockHelper.saveCredentialsInSmartLock > credentialsClient not connected!")

                    return
                }

                val credential = Credential.Builder(username)
                    .setPassword(password)
                    .setName(displayName)
                    .setProfilePictureUri(profilePicture)
                    .build()

                println("AMANDA-TEST > SmartLockHelper.saveCredentialsInSmartLock > created creds - passing to save")

                Auth.CredentialsApi.save(credentialsClient, credential).setResultCallback { status ->
                    println("AMANDA-TEST > SmartLockHelper.saveCredentialsInSmartLock > $status")

                    if (!status.isSuccess && status.hasResolution()) {
                        try {
                            getActivityAndCheckAvailability()?.let { activity ->
                                println("AMANDA-TEST > SmartLockHelper.saveCredentialsInSmartLock > prompting the user to save request")

                                // Prompt the user to resolve the save request
                                status.startResolutionForResult(activity, RequestCodes.SMART_LOCK_SAVE)
                            }
                        } catch (e: IntentSender.SendIntentException) {
                            // Could not resolve the request and therefore could not save
                            println("AMANDA-TEST > SmartLockHelper.saveCredentialsInSmartLock > error saving credentials")


                            WooLog.d(T.LOGIN, "SmartLock: unable to save credentials")
                        }
                    }
                }
            }
        }
    }

    fun deleteCredentialsInSmartLock(username: String, password: String) {
        getActivityAndCheckAvailability()?.let {
            credentialsClient?.let {
                if (!it.isConnected) {
                    return
                }

                val credential = Credential.Builder(username).setPassword(password).build()
                Auth.CredentialsApi.delete(credentialsClient, credential).setResultCallback { status ->
                    if (status.isSuccess) {
                        WooLog.i(T.LOGIN, "SmartLock: Credentials deleted successfully")
                    } else {
                        WooLog.i(T.LOGIN, "SmartLock: No saved credentials found for deletion")
                    }
                }
            }
        }
    }

    fun disableAutoSignIn() { Auth.CredentialsApi.disableAutoSignIn(credentialsClient) }
}
