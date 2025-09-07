package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.amitmatth.challengemonitor.databinding.FragmentOnboarding1Binding
import com.amitmatth.challengemonitor.ui.OnboardingActivity

class OnboardingFragment1 : Fragment() {

    private var _binding: FragmentOnboarding1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        (activity as? OnboardingActivity)?.setPageValidation(0, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}