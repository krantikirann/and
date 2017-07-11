/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofillframework.multidatasetservice

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.autofill.AutofillId
import com.example.android.autofillframework.CommonUtil.TAG
import com.example.android.autofillframework.CommonUtil.bundleToString
import com.example.android.autofillframework.R
import com.example.android.autofillframework.multidatasetservice.datasource.SharedPrefsAutofillRepository
import com.example.android.autofillframework.multidatasetservice.settings.MyPreferences
import java.util.Arrays

class MyAutofillService : AutofillService() {

    override fun onFillRequest(request: FillRequest, cancellationSignal: CancellationSignal,
            callback: FillCallback) {
        val structure = request.getFillContexts().get(request.getFillContexts().size - 1).structure
        val data = request.clientState
        Log.d(TAG, "onFillRequest(): data=" + bundleToString(data))
        cancellationSignal.setOnCancelListener { Log.w(TAG, "Cancel autofill not implemented in this sample.") }
        // Parse AutoFill data in Activity
        val parser = StructureParser(structure)
        parser.parseForFill()
        val autofillFields = parser.autofillFields

        val responseBuilder = FillResponse.Builder()
        // Check user's settings for authenticating Responses and Datasets.
        val responseAuth = MyPreferences.isResponseAuth(this)
        if (responseAuth && autofillFields.autofillIds.size > 0) {
            // If the entire Autofill Response is authenticated, AuthActivity is used
            // to generate Response.
            val sender = AuthActivity.getAuthIntentSenderForResponse(this)
            val presentation = AutofillHelper
                    .newRemoteViews(packageName, getString(R.string.autofill_sign_in_prompt), R.drawable.ic_lock_black_24dp)
            responseBuilder
                    .setAuthentication(autofillFields.autofillIds.toTypedArray(), sender, presentation)
            callback.onSuccess(responseBuilder.build())
        } else {
            val datasetAuth = MyPreferences.isDatasetAuth(this)
            val clientFormDataMap = SharedPrefsAutofillRepository.getFilledAutofillFieldCollection(this,
                    autofillFields.focusedAutofillHints, autofillFields.allAutofillHints)
            val response = AutofillHelper.newResponse(this, datasetAuth, autofillFields, clientFormDataMap)
            callback.onSuccess(response)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val context = request.fillContexts
        val structure = context[context.size - 1].structure
        val data = request.clientState
        Log.d(TAG, "onSaveRequest(): data=" + bundleToString(data))
        val parser = StructureParser(structure)
        parser.parseForSave()
        SharedPrefsAutofillRepository.saveFilledAutofillFieldCollection(this,
                parser.filledAutofillFieldCollection)
    }

    override fun onConnected() {
        Log.d(TAG, "onConnected")
    }

    override fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
    }
}
