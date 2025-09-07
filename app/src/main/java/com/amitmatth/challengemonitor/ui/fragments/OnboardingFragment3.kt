package com.amitmatth.challengemonitor.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentOnboarding3Binding
import com.amitmatth.challengemonitor.ui.OnboardingActivity
import com.amitmatth.challengemonitor.ui.listeners.OnboardingValidationListener

class OnboardingFragment3 : Fragment(), OnboardingValidationListener {

    private var _binding: FragmentOnboarding3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding3Binding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.nameInput.error = null
                (activity as? OnboardingActivity)?.setPageValidation(
                    2,
                    s?.toString()?.trim()?.isNotEmpty() == true
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.nameInput.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = binding.nameInput.compoundDrawables[2]
                if (drawableEnd != null) {
                    if (event.rawX >= (binding.nameInput.right - drawableEnd.bounds.width() - binding.nameInput.paddingRight)) {
                        if (isInputValid()) {
                            (activity as? OnboardingActivity)?.findViewById<ViewPager2>(R.id.viewPager)
                                ?.let { viewPager ->
                                    if (viewPager.currentItem == 2) {
                                        viewPager.setCurrentItem(3, true)
                                    }
                                }
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }

    override fun isInputValid(): Boolean {
        val name = binding.nameInput.text.toString().trim()
        val isValid = if (name.isEmpty()) {
            binding.nameInput.error = "Name cannot be empty"
            false
        } else {
            saveUserName(name)
            true
        }
        (activity as? OnboardingActivity)?.setPageValidation(2, isValid)
        return isValid
    }

    override fun onResume() {
        super.onResume()
        val currentName = binding.nameInput.text.toString().trim()
        val isValid = currentName.isNotEmpty()
        (activity as? OnboardingActivity)?.setPageValidation(2, isValid)
        if (isValid) {
            saveUserName(currentName)
        }
    }

    private fun saveUserName(userName: String) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("user_name", userName)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}