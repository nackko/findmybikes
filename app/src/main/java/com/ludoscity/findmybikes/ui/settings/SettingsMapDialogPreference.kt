package com.ludoscity.findmybikes.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.NumberPicker
import android.widget.TextView

import com.ludoscity.findmybikes.R
import com.ludoscity.findmybikes.data.database.SharedPrefHelper

/**
 * Created by F8Full on 2016-04-10. This file is part of #findmybikes
 * Provides a Setting dialog to configure map marker colors availability values
 * http://stackoverflow.com/questions/4505845/concise-way-of-writing-new-dialogpreference-classes
 */
class SettingsMapDialogPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private var mCriticalMaxPicker: NumberPicker? = null
    private var mBadMaxPicker: NumberPicker? = null
    private var mBadMinText: TextView? = null
    private var mGreatMinText: TextView? = null
    private var mCriticalHint: TextView? = null

    init {

        //TODO: deprecated in API level 29. Now in Jetpack. See branch settings-from-jetpack
        isPersistent = false
        dialogLayoutResource = R.layout.settings_map_dialog_content
    }

    @SuppressLint("StringFormatInvalid")
    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mCriticalMaxPicker = view.findViewById(R.id.pref_availability_critical_max_picker)
        mBadMaxPicker = view.findViewById(R.id.pref_availability_bad_max_picker)
        mBadMinText = view.findViewById(R.id.pref_availability_bad_min_text)
        mGreatMinText = view.findViewById(R.id.pref_availability_great_min_text)
        mCriticalHint = view.findViewById(R.id.pref_availability_critical_hint)

        val redUpperValue = SharedPrefHelper.getInstance().getCriticalAvailabilityMax(context)
        val yellowUpperValue = SharedPrefHelper.getInstance().getBadAvailabilityMax(context)


        mCriticalMaxPicker!!.minValue = 0
        mCriticalMaxPicker!!.maxValue = 3

        mBadMinText!!.text = String.format(context.getString(R.string.pref_availability_bad_min_label), redUpperValue + 1)
        (view.findViewById<View>(R.id.pref_availability_cri_min_text) as TextView).text = String.format(context.getString(R.string.pref_availability_cri_min_label), 0)

        mBadMaxPicker!!.minValue = redUpperValue + 1
        mBadMaxPicker!!.maxValue = redUpperValue + 4

        mCriticalMaxPicker!!.value = redUpperValue
        mBadMaxPicker!!.value = yellowUpperValue

        setupCriticalHint()

        mGreatMinText!!.text = String.format(context.getString(R.string.pref_availability_great_min_label), yellowUpperValue + 1)

        mCriticalMaxPicker!!.setOnValueChangedListener { numberPicker, i, _newValue ->
            setupCriticalHint()

            mBadMinText!!.text = String.format(context.getString(R.string.pref_availability_bad_min_label), _newValue + 1)

            mBadMaxPicker!!.minValue = _newValue + 1
            mBadMaxPicker!!.maxValue = _newValue + 4

            mGreatMinText!!.text = String.format(context.getString(R.string.pref_availability_great_min_label), mBadMaxPicker!!.value + 1)
        }

        mBadMaxPicker!!.setOnValueChangedListener { numberPicker, i, i1 -> mGreatMinText!!.text = String.format(context.getString(R.string.pref_availability_great_min_label), i1 + 1) }
    }

    private fun setupCriticalHint() {

        when (mCriticalMaxPicker!!.value) {
            0 -> mCriticalHint!!.setText(R.string.availability_hint_never)
            1 -> mCriticalHint!!.setText(R.string.availability_hint_sometime)
            2 -> mCriticalHint!!.setText(R.string.availability_hint_often)
            3 -> mCriticalHint!!.setText(R.string.availability_hint_veryoften)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {

        if (positiveResult) {
            SharedPrefHelper.getInstance().saveCriticalAvailabilityMax(context, mCriticalMaxPicker!!.value)
            SharedPrefHelper.getInstance().saveBadAvailabilityMax(context, mBadMaxPicker!!.value)
        }
        super.onDialogClosed(positiveResult)
    }
}
