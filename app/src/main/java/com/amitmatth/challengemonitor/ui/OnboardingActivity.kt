package com.amitmatth.challengemonitor.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.amitmatth.challengemonitor.databinding.ActivityOnboardingBinding
import com.amitmatth.challengemonitor.ui.adapter.OnboardingPagerAdapter
import com.amitmatth.challengemonitor.ui.fragments.*
import com.amitmatth.challengemonitor.ui.listeners.OnboardingValidationListener
import com.amitmatth.challengemonitor.utils.SnackbarUtils

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var pagerAdapter: OnboardingPagerAdapter
    private var previousPagePosition: Int = 0

    private val pageValidationStatus = mutableMapOf<Int, Boolean>()

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.currentItem -= 1
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                if (!isFinishing) {
                    isEnabled = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val screens = listOf(
            OnboardingFragment1(),
            OnboardingFragment2(),
            OnboardingFragment3(),
            OnboardingFragment4(),
            OnboardingFragment5()
        )

        pagerAdapter = OnboardingPagerAdapter(this, screens)
        binding.viewPager.adapter = pagerAdapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        pageValidationStatus[0] = true
        pageValidationStatus[1] = true
        pageValidationStatus[2] = false
        pageValidationStatus[3] = true
        pageValidationStatus[4] = false

        binding.viewPager.isUserInputEnabled = true 
        previousPagePosition = binding.viewPager.currentItem

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val currentFragment = supportFragmentManager.findFragmentByTag("f$previousPagePosition")
                var canProceed = true

                if (position > previousPagePosition) {
                    if (currentFragment is OnboardingValidationListener) {
                        if (!currentFragment.isInputValid()) {
                            canProceed = false
                            SnackbarUtils.showCustomSnackbar(this@OnboardingActivity, "Please complete the current step.")
                            binding.viewPager.setCurrentItem(previousPagePosition, false) 
                        }
                    }
                }

                if (canProceed) {
                    previousPagePosition = position
                } else {
                    if(binding.viewPager.currentItem != previousPagePosition){
                         binding.viewPager.setCurrentItem(previousPagePosition, false)
                    }
                }
            }
        })
    }

    fun setPageValidation(position: Int, isValid: Boolean) {
        pageValidationStatus[position] = isValid
    }

    fun completeOnboarding() {
        val pref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        pref.edit { putBoolean("onboarding_completed", true) }

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}