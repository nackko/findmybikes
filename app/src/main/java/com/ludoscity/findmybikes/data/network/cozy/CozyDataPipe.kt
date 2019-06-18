package com.ludoscity.findmybikes.data.network.cozy

import android.util.Log
import com.ludoscity.findmybikes.BuildConfig
import com.ludoscity.findmybikes.data.Result
import com.nimbusds.oauth2.sdk.*
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic
import com.nimbusds.oauth2.sdk.auth.Secret
import com.nimbusds.oauth2.sdk.client.ClientDeleteRequest
import com.nimbusds.oauth2.sdk.client.ClientRegistrationErrorResponse
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils
import com.nimbusds.openid.connect.sdk.*
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformationResponse
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata
import com.nimbusds.openid.connect.sdk.rp.OIDCClientRegistrationRequest
import com.nimbusds.openid.connect.sdk.rp.OIDCClientRegistrationResponseParser
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Created by F8Full on 2019-06-17. This file is part of #findmybikes
 *
 */
class CozyDataPipe {

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
                        clientId = clientInformation.id.value,
                        clientSecret = clientInformation.secret.value,
                        registrationAccessToken = clientInformation.registrationAccessToken.value
                )
        )
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

        var tokenHTTPResp: HTTPResponse? = null
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
            tokenResponse = OIDCTokenResponseParser.parse(tokenHTTPResp!!)
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
            Log.d(TAG, "Getting cozy network data pipe")
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