package com.amitmatth.challengemonitor.utils

import android.app.Activity
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.amitmatth.challengemonitor.R
import com.google.android.material.snackbar.Snackbar

object SnackbarUtils {

    private const val DEFAULT_MARGIN_BOTTOM = 100

    fun showCustomSnackbar(activity: Activity, message: String, duration: Int = 3500) {
        val snackbar = Snackbar.make(
            activity.findViewById(android.R.id.content),
            "",
            Snackbar.LENGTH_INDEFINITE
        )

        val snackbarLayout = snackbar.view as ViewGroup
        val customView = activity.layoutInflater.inflate(
            R.layout.custom_snackbar,
            snackbarLayout,
            false
        )

        val messageText = customView.findViewById<TextView>(R.id.snackbar_message)
        val closeButton = customView.findViewById<ImageView>(R.id.snackbar_close)

        messageText.text = message
        closeButton.setOnClickListener { snackbar.dismiss() }

        snackbarLayout.setBackgroundColor(Color.TRANSPARENT)
        snackbarLayout.setPadding(0, 0, 0, 0)

        snackbarLayout.removeAllViews()
        snackbarLayout.addView(customView)

        val params = snackbar.view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(
            params.leftMargin,
            params.topMargin,
            params.rightMargin,
            DEFAULT_MARGIN_BOTTOM
        )
        snackbar.view.layoutParams = params

        snackbar.show()

        snackbar.view.postDelayed({
            snackbar.dismiss()
        }, duration.toLong())
    }
}