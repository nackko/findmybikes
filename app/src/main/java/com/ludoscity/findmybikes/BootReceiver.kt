package com.ludoscity.findmybikes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ludoscity.findmybikes.data.FindMyBikesRepository
import com.ludoscity.findmybikes.data.geolocation.TransitionRecognitionService
import com.ludoscity.findmybikes.utils.Utils
import com.ludoscity.findmybikes.utils.intentFor
import com.ludoscity.findmybikes.utils.startServiceForeground

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            //Not great - Can't retrieve repo and check isLoggedInCozy value because deserialization
            //happens asynchronously after repo construction. Going directly to data source
            //Better approach could be observing repo isLoggedInCozy (LiveData) with a timeout
            if (Utils.getSecureSharedPref(ctx).contains(FindMyBikesRepository.OAUTH_ACCESS_TOKEN_PREF_KEY)) {
                ctx.startServiceForeground(ctx.intentFor<TransitionRecognitionService>())
            }
        }
    }
}