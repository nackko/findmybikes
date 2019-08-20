package com.ludoscity.findmybikes.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.Result
import com.ludoscity.findmybikes.data.network.cozy.CozyCloudAPI
import com.ludoscity.findmybikes.ui.table.TableFragmentViewModel
import okhttp3.OkHttpClient
import org.jetbrains.anko.bundleOf
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by F8Full on 2015-04-30.
 *
 * Class with static utilities
 */
fun Location.asLatLng(): LatLng = LatLng(latitude, longitude)

fun LatLng.asString(): String = "$latitude|$longitude"

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

/**
 * Extension function to start foreground services
 *
 * @param service   the intent of service to be started
 */
fun Context.startServiceForeground(service: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(service)
    } else {
        startService(service)
    }
}

/**
 * Extension function to create intents
 *
 * @param action    the action to be added to the intent
 * @param flags     the flags to be added to the intent
 * @param extras    the extras to be added to the intent
 * @return          the created intent
 */
inline fun <reified T : Any> Context.intentFor(action: String? = null,
                                               flags: Array<Int>? = null,
                                               vararg extras: Pair<String, Any>): Intent =
        Intent(this, T::class.java).apply {
            if (action != null) setAction(action)
            flags?.forEach { setFlags(it) }
            putExtras(bundleOf(*extras))
        }
object Utils {

    val TAG = Utils::class.java.simpleName

    fun getTracingNotificationTitle(ctx: Context): String {
        return "Waiting for next bike trip"
        //return ctx.getString(R.string.location_updated,
        //        DateFormat.getDateTimeInstance().format(Date()))
    }

    fun getSimpleDateFormatPattern(): String {
        //see: https://developer.android.com/reference/java/text/SimpleDateFormat
        //https://stackoverflow.com/questions/28373610/android-parse-string-to-date-unknown-pattern-character-x
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        else
            "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"
    }

    fun toISO8601UTC(date: Date?): String? {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
        df.timeZone = tz
        return if (date != null) df.format(date) else null
    }

    fun fromISO8601UTC(dateStr: String): Date? {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US)
        df.timeZone = tz

        var toReturn: Date? = null

        try {
            toReturn = df.parse(dateStr)
        } catch (e: Exception) {

        }

        return toReturn

    }

    fun getCozyCloudAPI(ctx: Context): CozyCloudAPI {
        //TODO: code duplicated in CozyPipeDataIntentService::onCreate
        //see: https://www.coderdump.net/2018/04/automatic-refresh-api-token-with-retrofit-and-okhttp-authenticator.html
        val httpClientBuilder = OkHttpClient.Builder()


        //interceptor to add authorization header with token to every request
        httpClientBuilder.addInterceptor {
            var response = it.proceed(it.request().newBuilder().addHeader("Authorization",
                    "Bearer ${InjectorUtils.provideRepository(ctx).userCred?.accessToken}").build()
            )

            //When access token is expired, cozy replies with code 400 -- Bad request
            if (response.code() == 400) {
                if (response.body()?.string()?.equals("Expired token") == true) {
                    Log.i(TAG, "Captured 400 error Expired token - initiating token refresh")
                    val refreshResult = InjectorUtils.provideRepository(ctx).refreshCozyAccessToken()

                    //We're clear to retry the original request from it.request
                    if (refreshResult is Result.Success)
                        response = it.proceed(it.request().newBuilder().addHeader("Authorization", "Bearer ${refreshResult.data.accessToken}").build())
                }
            }

            response
        }

        //authenticator to grab 401 errors, refresh access token and retry the original request
        httpClientBuilder.authenticator { _, response ->

            Log.i(TAG, "Captured 401 error - initiating token refresh")
            val refreshResult = InjectorUtils.provideRepository(ctx).refreshCozyAccessToken()

            if (refreshResult is Result.Success)
                response.request().newBuilder().addHeader("Authorization", "Bearer ${refreshResult.data.accessToken}").build()
            else
                null
        }

        Log.d(TAG, "Building a Cozy API instance")
        return Retrofit.Builder()
                //TODO: should auth also happen through this intent service ?
                .baseUrl(InjectorUtils.provideRepository(ctx).cozyOAuthClient?.stackBaseUrl!!)
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CozyCloudAPI::class.java)
    }

    private const val sharedPrefFilename = "findmybikes_secure_prefs"

    fun getSecureSharedPref(ctx: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences
                .create(
                        sharedPrefFilename,
                        masterKeyAlias,
                        ctx,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
    }

    fun getBikeSpeedPaddedBounds(ctx: Context, boundsToPad: LatLngBounds): LatLngBounds {
        return padLatLngBounds(boundsToPad, getAverageBikingSpeedKmh(ctx).toDouble())
    }

    private fun padLatLngBounds(boundsIn: LatLngBounds, paddingKm: Double): LatLngBounds {

        //http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
        //http://stackoverflow.com/questions/29478463/offset-latlng-by-some-amount-of-meters-in-android
        //Latitude : easy, 1 degree = 111111m (historically because of the French :D)
        //Longitude : 1 degree = 111111 * cos (latitude)m
        val southwestPadded = LatLng(boundsIn.southwest.latitude - (paddingKm * 1000.0) / 111111.0,
                boundsIn.southwest.longitude - (paddingKm * 1000.0) / 111111.0 * Math.cos(boundsIn.southwest.latitude))
        val northeastPadded = LatLng(boundsIn.northeast.latitude + (paddingKm * 1000.0) / 111111.0,
                boundsIn.northeast.longitude + (paddingKm * 1000.0) / 111111.0 * Math.cos(boundsIn.northeast.latitude))

        return LatLngBounds(southwestPadded, northeastPadded)
    }

    fun getExponentialDelayMillis(nbAttempt: Int): Long {
        return Math.round(Math.pow(2.0, nbAttempt.toDouble())) * 1000
    }

    fun extractNearestAvailableStationIdFromDataString(_processedString: String): String {

        //int debug0 = _processedString.indexOf(StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX);
        //int debug1 = StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length();
        //int debug2 = _processedString.length();


        //Either a station id followed by _AVAILABILITY_AOK
        //or
        //a station id followed by _AVAILABILITY_BAD
        //or
        //a station id followed by _AVAILABILITY_LCK

        //extract only first id
        val firstId = extractOrderedStationIdsFromProcessedString(_processedString)[0]

        return if (firstId.length >= 32) firstId.substring(0, 32) else ""


        //everything went AOK
        /*if (_processedString.indexOf(StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX) != -1 &&
                _processedString.indexOf(StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX) + StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() ==
                        _processedString.length()){

            return _processedString.substring(0, _processedString.length() - StationTableRecyclerViewAdapter.AOK_AVAILABILITY_POSTFIX.length() );

        }
        else {
            int debug3 = _processedString.lastIndexOf(StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE);

            //some availability troubles, let's just trim the end
            return _processedString.substring(0, _processedString.lastIndexOf(StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE));
        }*/
    }

    fun cleanStringForHashtagUse(toClean: String): String {
        var hashtagableBikeSystemName = toClean
        hashtagableBikeSystemName = hashtagableBikeSystemName.replace("\\s".toRegex(), "")
        hashtagableBikeSystemName = hashtagableBikeSystemName.replace("[^A-Za-z0-9 ]".toRegex(), "")
        hashtagableBikeSystemName = hashtagableBikeSystemName.toLowerCase()

        return hashtagableBikeSystemName
    }

    //citybik.es Ids, ordered by distance
    //get(0) is the id of the selected station with BAD or AOK availability
    fun extractOrderedStationIdsFromProcessedString(_processedString: String): ArrayList<String> {

        if (_processedString.isEmpty()) {
            val toReturn = ArrayList<String>()
            toReturn.add(_processedString)

            return toReturn
        }

        //int startSequenceIdx = _processedString.lastIndexOf(StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE);

        /*int subStringStarIdxDebug = _processedString.lastIndexOf(StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE)
                + StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length();*/

        /*String subStringDebug = _processedString.substring(_processedString.lastIndexOf(StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE)
                + StationTableRecyclerViewAdapter.AVAILABILITY_POSTFIX_START_SEQUENCE.length());*/


        //TODO: something is fishy here, couldn't figure out how to get the same result without intermediary debug labelled variable
        val debugSplit = _processedString.substring(_processedString.indexOf(TableFragmentViewModel.AVAILABILITY_POSTFIX_START_SEQUENCE) + TableFragmentViewModel.AOK_AVAILABILITY_POSTFIX.length)

        //String[] debugSplitResult = debugSplit.split(String.format("(?<=\\G.{%d})", StationTableRecyclerViewAdapter.CRITICAL_AVAILABILITY_POSTFIX.length() + 32));

        val toReturn = ArrayList<String>()
        toReturn.add(_processedString.substring(0, 32 + TableFragmentViewModel.AOK_AVAILABILITY_POSTFIX.length))

        toReturn.addAll(splitEqually(debugSplit, TableFragmentViewModel.CRITICAL_AVAILABILITY_POSTFIX.length + 32))

        return toReturn
    }

    private fun splitEqually(text: String, size: Int): List<String> {
        // Give the list the right capacity to start with. You could use an array
        // instead if you wanted.
        val ret = ArrayList<String>((text.length + size - 1) / size)

        var start = 0
        while (start < text.length) {
            ret.add(text.substring(start, Math.min(text.length, start + size)))
            start += size
        }
        return ret
    }

    //workaround from https://code.google.com/p/gmaps-api-issues/issues/detail?id=9011
    fun getBitmapDescriptor(ctx: Context, id: Int): BitmapDescriptor {
        val vectorDrawable = ResourcesCompat.getDrawable(ctx.resources, id, null)
        val bm = Bitmap.createBitmap(vectorDrawable!!.intrinsicWidth,
                vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bm)
    }

    fun map(x: Float, in_min: Float, in_max: Float, out_min: Float, out_max: Float): Float {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    }

    fun dpToPx(toConvert: Float, ctx: Context): Int {
        /// Converts 66 dip into its equivalent px
        val r = ctx.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toConvert, r.displayMetrics).toInt()
    }

    //http://stackoverflow.com/questions/3282390/add-floating-point-value-to-android-resources-values
    fun getAverageWalkingSpeedKmh(ctx: Context): Float {
        val outValue = TypedValue()

        ctx.resources.getValue(R.dimen.average_walking_speed_kmh, outValue, true)

        return outValue.float
    }

    fun getAverageBikingSpeedKmh(ctx: Context): Float {
        val outValue = TypedValue()

        ctx.resources.getValue(R.dimen.average_biking_speed_kmh, outValue, true)

        return outValue.float
    }

    fun getWalkingProximityString(from: LatLng?, to: LatLng?, _2digitsFormat: Boolean, nf: NumberFormat, ctx: Context): String? {
        return getProximityString(from, to, getAverageWalkingSpeedKmh(ctx), _2digitsFormat, nf, ctx)
    }

    fun getBikingProximityString(_from: LatLng, _to: LatLng, _2digitsFormat: Boolean, _nf: NumberFormat, _ctx: Context): String? {
        return getProximityString(_from, _to, getAverageBikingSpeedKmh(_ctx), _2digitsFormat, _nf, _ctx)
    }

    fun getWalkingDurationBetweenInMinutes(from: LatLng?, to: LatLng?, ctx: Context): Int? {

        from?.let {
            to?.let {
                return computeTimeBetweenInMinutes(from, to, getAverageWalkingSpeedKmh(ctx))
            }
        }
        return null
    }

    fun getBikingDurationBetweenInMinutes(from: LatLng?, to: LatLng?, ctx: Context): Int? {
        from?.let {
            to?.let {
                return computeTimeBetweenInMinutes(from, to, getAverageBikingSpeedKmh(ctx))
            }
        }
        return null
    }

    private fun getProximityString(_from: LatLng?, _to: LatLng?, _speedKmh: Float, _2digitsFormat: Boolean, _nf: NumberFormat, _ctx: Context): String? {

        return durationToProximityString(computeTimeBetweenInMinutes(_from, _to, _speedKmh), _2digitsFormat, _nf, _ctx)

    }

    fun durationToProximityString(durationMinute: Int?, twoDigitsFormat: Boolean, nf: NumberFormat, ctx: Context): String? {

        var toReturn: String? = null

        //TODO: shouldn't the person doing the calling configure the NumberFormat ?
        if (twoDigitsFormat)
            nf.minimumIntegerDigits = 2
        else
            nf.minimumIntegerDigits = 1

        durationMinute?.let {
            toReturn = when {
                durationMinute < 1 -> "<" + nf.format(1) + ctx.getString(R.string.min_abbreviated)
                durationMinute < 60 -> "~" + nf.format(durationMinute.toLong()) + ctx.getString(R.string.min_abbreviated)
                else -> "> ${nf.format(1)}${ctx.getString(R.string.hour_abbreviated)}"
            }
        }

        return toReturn
    }

    fun computeTimeBetweenInMinutes(from: LatLng?, to: LatLng?, speedKmH: Float): Int {

        var toReturn = 0

        from?.let {
            to?.let {
                val distance = SphericalUtil.computeDistanceBetween(from, to).toInt()

                val speedMetersPerH = speedKmH * 1000f
                val speedMetersPerS = speedMetersPerH / 3600f

                val timeInS = distance / speedMetersPerS

                val timeInMs = (timeInS * 1000).toLong()

                toReturn = (timeInMs / 1000 / 60).toInt()
            }
        }

        return toReturn
    }

    /**
     * Round to certain number of decimals
     *
     * @param d
     * @param decimalPlace
     * @return
     */
    private fun round(d: Float, decimalPlace: Int): Float {
        var bd = BigDecimal(java.lang.Float.toString(d))
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP)
        return bd.toFloat()
    }

    /**
     * Returns a percentage value as a float from an XML resource file. The value can be optionally
     * rounded.
     *
     * @param _ctx
     * @param _resId
     * @param _rounded
     * @return float
     */
    fun getPercentResource(_ctx: Context, _resId: Int, _rounded: Boolean): Float {
        val valueContainer = TypedValue()
        _ctx.resources.getValue(_resId, valueContainer, true)
        var toReturn = valueContainer.getFraction(1f, 1f)//http://stackoverflow.com/questions/11734470/how-does-one-use-resources-getfraction

        if (_rounded)
            toReturn = round(toReturn, 2)

        return toReturn
    }

    fun fromHtml(html: String): Spanned {
        val result: Spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            result = Html.fromHtml(html)
        }
        return result
    }

    //Snackbar related utils
    object Snackbar {


        //A modified version of make that allows background color manipulation
        //as it is not currently supported through styles/theming
        //Should be done something like this
        /*<style name="MyCustomSnackbar" parent="Theme.AppCompat.Light">
        <item name="colorAccent">@color/theme_accent</item>
        <item name="android:textColor">@color/theme_textcolor_primary</item>
        <item name="android:background">@color/theme_primary_dark</item>
        </style>*/
        //Right now, textColor and action color are controlled through theming,
        //but not background color.
        fun makeStyled(_view: View, @StringRes _textStringResId: Int, _duration: Int,
                       @ColorInt _backgroundColor: Int/*, @ColorInt int _textColor, @ColorInt int _actionTextColor*/): com.google.android.material.snackbar.Snackbar {

            val toReturn = com.google.android.material.snackbar.Snackbar.make(_view, _textStringResId, _duration)

            val snackbarView = toReturn.view

            //didn't use to work but maybe newer SnackBar versions will support it ?
            /*//change snackbar action text color
            toReturn.setActionTextColor(_actionTextColor);

            // change snackbar text color
            int snackbarTextId = android.support.design.R.id.snackbar_text;
            TextView textView = (TextView)snackbarView.findViewById(snackbarTextId);
            textView.setTextColor(_textColor);*/

            // change snackbar background
            snackbarView.setBackgroundColor(_backgroundColor)

            return toReturn
        }

    }
}