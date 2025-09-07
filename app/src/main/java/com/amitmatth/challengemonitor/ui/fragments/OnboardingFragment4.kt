package com.amitmatth.challengemonitor.ui.fragments

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentOnboarding4Binding
import com.amitmatth.challengemonitor.ui.OnboardingActivity
import com.amitmatth.challengemonitor.ui.listeners.OnboardingValidationListener
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class OnboardingFragment4 : Fragment(), OnboardingValidationListener {

    private var _binding: FragmentOnboarding4Binding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    try {
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri,
                            takeFlags
                        )

                        saveProfileImageUri(uri)
                        loadProfileImage(uri)
                    } catch (_: SecurityException) {
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Failed to get permission for image."
                        )
                    }
                    (activity as? OnboardingActivity)?.setPageValidation(3, true)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboarding4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSavedProfileImage()

        binding.profileImageView.setOnClickListener {
            openGalleryForImage()
        }

        binding.editProfileIcon.setOnClickListener {
            openGalleryForImage()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            pickImageLauncher.launch(intent)
        } catch (_: Exception) {
            SnackbarUtils.showCustomSnackbar(requireActivity(), "No app found to pick image")
        }
    }

    private fun loadProfileImage(uri: Uri?) {
        uri?.let {
            Glide.with(this)
                .load(it)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.outline_account_circle_24)
                .error(R.drawable.outline_account_circle_24)
                .into(binding.profileImageView)
        }
    }

    private fun saveProfileImageUri(uri: Uri) {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString("profile_image_uri", uri.toString())
        }
    }

    private fun loadSavedProfileImage() {
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("profile_image_uri", null)
        if (uriString != null) {
            selectedImageUri = uriString.toUri()
            loadProfileImage(selectedImageUri)
        } else {
            Glide.with(this)
                .load(R.drawable.outline_account_circle_24)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.profileImageView)
        }
        (activity as? OnboardingActivity)?.setPageValidation(3, true)
    }

    override fun isInputValid(): Boolean {
        return true
    }

    override fun onResume() {
        super.onResume()
        (activity as? OnboardingActivity)?.setPageValidation(3, true)
        loadSavedProfileImage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}