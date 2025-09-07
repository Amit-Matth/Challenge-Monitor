package com.amitmatth.challengemonitor.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentOnboarding2Binding
import com.amitmatth.challengemonitor.ui.OnboardingActivity

class OnboardingFragment2 : Fragment() {

    private var _binding: FragmentOnboarding2Binding? = null
    private val binding get() = _binding!!
    private var shakeAnimation: Animation? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shakeAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)

        binding.privacyIconImageView.startAnimation(shakeAnimation)

        binding.privacyIconImageView.setOnClickListener {
            it.startAnimation(shakeAnimation)
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? OnboardingActivity)?.setPageValidation(1, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        shakeAnimation = null
    }
}