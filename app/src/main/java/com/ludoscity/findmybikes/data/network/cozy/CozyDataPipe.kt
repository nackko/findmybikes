package com.ludoscity.findmybikes.data.network.cozy

import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.ludoscity.findmybikes.BuildConfig
import com.ludoscity.findmybikes.data.Result
import com.ludoscity.findmybikes.data.database.tracking.AnalTrackingDatapoint
import com.ludoscity.findmybikes.data.database.tracking.BaseTrackingDatapoint
import com.ludoscity.findmybikes.data.database.tracking.GeoTrackingDatapoint
import com.ludoscity.findmybikes.utils.Utils
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.client.ClientDeleteRequest
import com.nimbusds.oauth2.sdk.client.ClientRegistrationErrorResponse
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformationResponse
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata
import com.nimbusds.openid.connect.sdk.rp.OIDCClientRegistrationRequest
import com.nimbusds.openid.connect.sdk.rp.OIDCClientRegistrationResponseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import retrofit2.Response
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
class CozyDataPipe {

    private val coroutineScopeIO = CoroutineScope(Dispatchers.IO)

    fun register(cozyBaseUrlString: String): Result<RegisteredOAuthClient> {

        //TODO: from a configuration file like the AppAuth Android demo app
        val jsonMetadata =
                "{\"redirect_uris\":[\"findmybikes://com.f8full.findmybikes.oauth2redirect\"],\"client_name\":\"#findmybikes\",\"software_id\":\"github.com/f8full/findmybikes\",\"software_version\":\"${BuildConfig.VERSION_NAME}\",\"client_kind\":\"mobile\",\"client_uri\":\"https://play.google.com/store/apps/details?id=com.ludoscity.findmybikes\",\"logo_uri\":\"https://drive.google.com/uc?export=download&id=1lhBnz4Fq_uNldcMd_1NwGC1I4RC0obgh\",\"policy_uri\":\"https://github.com/f8full/findmybikes/blob/master/Privacy_policy.md\"}"
        val metadata = OIDCClientMetadata.parse(JSONObjectUtils.parse(jsonMetadata))

// Make registration request
        val registrationRequest =
                OIDCClientRegistrationRequest(URI("$cozyBaseUrlString/auth/register"), metadata, null)

        //Special sauce for Cozy : it will not accept any request if accept header is not set
        val cozySpecialSauce = registrationRequest.toHTTPRequest()

        cozySpecialSauce.accept = "application/json"

        try {
            val regHTTPResponse = cozySpecialSauce.send()

// Parse and check response
            val registrationResponse = OIDCClientRegistrationResponseParser.parse(regHTTPResponse)

            if (registrationResponse is ClientRegistrationErrorResponse) {
                val error = registrationResponse
                        .errorObject
                return Result.Error(IOException("Error registering client : ${error.toJSONObject()}"))
            }

            val clientInformation = (registrationResponse as OIDCClientInformationResponse).oidcClientInformation

            return Result.Success(
                    RegisteredOAuthClient(
                            stackBaseUrl = cozyBaseUrlString,
                            clientId = clientInformation.id.value,
                            clientSecret = clientInformation.secret.value,
                            registrationAccessToken = clientInformation.registrationAccessToken.value
                    )
            )
        } catch (e: IOException) {
            return Result.Error(e)
        }

    }

    fun unregister(cozyBaseUrlString: String,
                   clientId: String,
                   masterAccessToken: String): Result<Boolean> {


        val req = ClientDeleteRequest(URI("$cozyBaseUrlString/auth/register/$clientId"),
                BearerAccessToken(masterAccessToken)
        )

        val deleteReponse = req.toHTTPRequest().send()

        if (!deleteReponse.indicatesSuccess()) {
            // We have an error
            val error = ClientRegistrationErrorResponse.parse(deleteReponse)
                    .errorObject
            return Result.Error(IOException("Error registering client : ${error.toJSONObject()}"))
        }

        return Result.Success(true)
    }

    fun update() {

    }

    fun info() {

    }


    /////////////////////////////
    //FILES API

    private val _mkDirResult: MutableLiveData<Result<CozyFileDescAnswerRoot>> = MutableLiveData()

    val createDirectoryResult: LiveData<Result<CozyFileDescAnswerRoot>> = _mkDirResult


    fun postDirectory(api: CozyCloudAPI,
                      dirName: String,
            //TODO: add parent directory id ?
                      dirTagCollection: List<String>) {

        val queryMap = HashMap<String, String>()

        queryMap["Type"] = "directory"
        queryMap["Name"] = dirName
        queryMap["Tags"] = dirTagCollection.joinToString(prefix = "[", postfix = "]")
        val call = api.postFile(specifications = queryMap)

        val createAnswer: Response<CozyFileDescAnswerRoot>

        try {
            createAnswer = call.execute()

            when (createAnswer.code()) {
                201 -> _mkDirResult.postValue(Result.Success(createAnswer.body()!!))
                409 -> {    //folder already exist
                    Log.w(TAG, "Directory '$dirName' already exist." +
                            " Attempting existing directory id retrieval via metadata...")
                    val metadataCall = api.getFileMetadata("/$dirName")

                    val metadataAnswer: Response<CozyFileDescAnswerRoot>

                    try {
                        metadataAnswer = metadataCall.execute()

                        if (metadataAnswer.code() == 200) {
                            //we recovered
                            Log.i(TAG, "...SUCCESS :)")
                            _mkDirResult.postValue(Result.Success(metadataAnswer.body()!!))
                        } else {
                            val error = metadataAnswer.errorBody()
                            _mkDirResult.postValue(Result.Error(IOException("...FAILURE ><" +
                                    "with error: $error")))

                        }
                    } catch (e: Exception) {
                        _mkDirResult.postValue(Result.Error(e))
                    }
                }
                else -> {
                    val error = createAnswer.errorBody()
                    _mkDirResult.postValue(Result.Error(IOException("Unexpected error " +
                            "when creating cozy '$dirName' directory: $error ")))
                }
            }

        } catch (e: Exception) {
            _mkDirResult.postValue(Result.Error(e))
        }
    }

    fun postFile(fileContent: BaseTrackingDatapoint,
                 gson: Gson,
                 api: CozyCloudAPI,
                 parentDirectoryId: String? = "",
                 fileName: String,
                 metadataId: String = "",
                 createdAt: String = "",
                 fileTagCollection: List<String>): Result<Boolean> {

        val queryMap = HashMap<String, String>()

        queryMap["Type"] = "file"
        queryMap["Name"] = fileName
        queryMap["Tags"] = fileTagCollection.joinToString(prefix = "[", postfix = "]")
        if (!metadataId.isBlank())
            queryMap["MetadataID"] = metadataId

        queryMap["CreatedAt"] = if (createdAt.isNotEmpty()) createdAt else {
            val createdAtOriginal = SimpleDateFormat(Utils.getSimpleDateFormatPattern(), Locale.US)
                    .parse(fileContent.timestamp)
            Utils.toISO8601UTC(createdAtOriginal) ?: ""
        }

        //Calculate MD5 hash required by cozy server
        var base64MD5 = Base64.encodeToString(
                MessageDigest.getInstance("MD5")
                        .digest(gson.toJson(fileContent).toByteArray(Charsets.UTF_8)),
                Base64.DEFAULT)

        base64MD5 = base64MD5.replace("\n", "")

        val call = when (fileContent) {
            is AnalTrackingDatapoint -> api.postFile(fileContent,
                    parentDirectoryId = parentDirectoryId ?: "",
                    specifications = queryMap,
                    contentMD5 = base64MD5
            )
            is GeoTrackingDatapoint -> api.postFile(fileContent,
                    parentDirectoryId = parentDirectoryId ?: "",
                    specifications = queryMap,
                    contentMD5 = base64MD5
            )
            else -> null
        }

        val createAnswer: Response<CozyFileDescAnswerRoot>?

        var toReturn: Result<Boolean>

        try {
            createAnswer = call?.execute()

            when (createAnswer?.code()) {
                201 -> {
                    Log.i(TAG, "1 file uploaded")
                    toReturn = Result.Success(true)
                }
                409 -> {    //folder already exist
                    Log.w(TAG, "File '$fileName' already exist. Aborted")
                    toReturn = Result.Error(IOException("Code 409, File '$fileName' already exist. Aborted"))
                }
                else -> {
                    val error = createAnswer?.errorBody()
                    toReturn = Result.Error(IOException("Unexpected error " +
                            "when creating cozy '$fileName' file: ${error.toString()} "))

                    Log.e(TAG, error?.string() ?: "")
                }
            }

        } catch (e: Exception) {
            toReturn = Result.Error(e)
        }

        return toReturn
    }

    /////////////////////////////////

    fun refreshAccessToken(cozyBaseUrlString: String,
                           expiredCred: UserCredentialTokens,
                           clientId: String,
                           clientSecret: String
    ): Result<UserCredentialTokens> {

        Log.i(TAG, "About to refresh access token using refresh token")

        val tokenReq = TokenRequest(
                URI("$cozyBaseUrlString/auth/access_token"),
                ClientSecretBasic(
                        ClientID(clientId),
                        Secret(clientSecret)
                ),
                RefreshTokenGrant(RefreshToken(expiredCred.refreshToken))
        )

        val tokenHTTPResp: HTTPResponse?
        try {
            //TODO: Find how to properly construct the TokenRequest object so that the request doesn't have to be altered here
            val truc = tokenReq.toHTTPRequest()

            truc.query = "${truc.query}&client_id=$clientId&client_secret=$clientSecret"
            //tokenHTTPResp = tokenReq.toHTTPRequest().send()
            tokenHTTPResp = truc.send()
        } catch (e: SerializeException) {
            return Result.Error(e)
        } catch (e: IOException) {
            return Result.Error(e)
        }

        // Parse and check response
        val tokenResponse: TokenResponse?
        try {
            tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp)
        } catch (e: ParseException) {
            return Result.Error(e)
        }


        if (tokenResponse is TokenErrorResponse) {
            val error = tokenResponse.errorObject
            return Result.Error(IOException("Error while refreshing token : ${error.toJSONObject()}"))
        }

        val accessTokenResponse = tokenResponse as OIDCTokenResponse?

        return Result.Success(
                UserCredentialTokens(
                        accessToken = accessTokenResponse?.oidcTokens?.accessToken?.value!!,
                        refreshToken = expiredCred.refreshToken//,
                        //idToken = accessTokenResponse.oidcTokens?.idTokenString!!
                )
        )
    }

    fun exchangeAuthCodeForTokenCouple(
            cozyBaseUrlString: String,
            redirectIntentData: String,
            clientId: String,
            clientSecret: String,
            authRequestState: State
    ): Result<UserCredentialTokens> {

        val authResp: AuthenticationResponse?

        try {
            authResp = AuthenticationResponseParser.parse(URI(redirectIntentData.removeSuffix("#")))
        } catch (e: ParseException) {
            return Result.Error(e)

        } catch (e: URISyntaxException) {
            return Result.Error(e)
        }

        if (authResp is AuthenticationErrorResponse) {
            val error = authResp.errorObject
            return Result.Error(IOException("Error while authenticating : ${error.toJSONObject()}"))
        }

        val successResponse = authResp as AuthenticationSuccessResponse

        /* Don't forget to check the state!
 * The state in the received authentication response must match the state
 * specified in the previous outgoing authentication request.
 *
 *
*/
        if (successResponse.state != authRequestState) {
            return Result.Error(IOException("Auth request state validation failed"))
        }

        val authCode = successResponse.authorizationCode

        val tokenReq = TokenRequest(
                URI("$cozyBaseUrlString/auth/access_token"),
                ClientSecretBasic(
                        ClientID(clientId),
                        Secret(clientSecret)
                ),
                //TODO: from config file
                AuthorizationCodeGrant(authCode, URI("findmybikes://com.f8full.findmybikes.oauth2redirect"))
        )

        val tokenHTTPResp: HTTPResponse?
        try {
            //TODO: Find how to properly construct the TokenRequest object so that the request doesn't have to be altered here
            val truc = tokenReq.toHTTPRequest()

            truc.query = "${truc.query}&client_id=$clientId&client_secret=$clientSecret"
            //tokenHTTPResp = tokenReq.toHTTPRequest().send()
            tokenHTTPResp = truc.send()
        } catch (e: SerializeException) {
            return Result.Error(e)
        } catch (e: IOException) {
            return Result.Error(e)
        }


        // Parse and check response
        val tokenResponse: TokenResponse?
        try {
            tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp)
        } catch (e: ParseException) {
            return Result.Error(e)
        }


        if (tokenResponse is TokenErrorResponse) {
            val error = tokenResponse.errorObject
            return Result.Error(IOException("Error while authenticating : ${error.toJSONObject()}"))
        }

        val accessTokenResponse = tokenResponse as OIDCTokenResponse?

        return Result.Success(
                UserCredentialTokens(
                        accessToken = accessTokenResponse?.oidcTokens?.accessToken?.value!!,
                        refreshToken = accessTokenResponse.oidcTokens?.refreshToken?.value!!//,
                        //idToken = accessTokenResponse.oidcTokens?.idTokenString!!
                )
        )
    }

    companion object {
        private val TAG = CozyDataPipe::class.java.simpleName

        // For Singleton instantiation
        private val LOCK = Any()
        private var sInstance: CozyDataPipe? = null

        /**
         * Get the singleton for this class
         */
        fun getInstance(): CozyDataPipe {
            //Log.d(TAG, "Getting cozy network data pipe")
            if (sInstance == null) {
                synchronized(LOCK) {
                    sInstance = CozyDataPipe()
                    Log.d(TAG, "Made new Cozy network data pipe")
                }
            }
            return sInstance!!
        }
    }
}