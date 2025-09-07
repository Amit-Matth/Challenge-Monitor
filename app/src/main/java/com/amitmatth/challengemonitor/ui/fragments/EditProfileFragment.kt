package com.amitmatth.challengemonitor.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.amitmatth.challengemonitor.R
import com.amitmatth.challengemonitor.databinding.FragmentEditProfileBinding
import com.amitmatth.challengemonitor.ui.MainActivity
import com.amitmatth.challengemonitor.utils.SnackbarUtils
import com.bumptech.glide.Glide
import androidx.core.content.edit

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        requireContext().contentResolver.takePersistableUriPermission(
                            uri,
                            takeFlags
                        )
                        selectedImageUri = uri
                        Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .placeholder(R.drawable.outline_account_circle_24)
                            .error(R.drawable.outline_account_circle_24)
                            .into(binding.editProfileImageView)
                    } catch (_: SecurityException) {
                        SnackbarUtils.showCustomSnackbar(
                            requireActivity(),
                            "Failed to secure image permission."
                        )
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfileData()

        binding.changeProfileImageButton.setOnClickListener {
            openGalleryForImage()
        }

        binding.editProfileImageView.setOnClickListener {
            openGalleryForImage()
        }

        binding.saveProfileButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            pickImageLauncher.launch(intent)
        } catch (_: Exception) {
            SnackbarUtils.showCustomSnackbar(requireActivity(), "No app found to pick image")
        }
    }

    private fun loadProfileData() {
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("user_name", "")
        val imageUriString = prefs.getString("profile_image_uri", null)

        binding.nameEditText.setText(currentName)

        imageUriString?.let {
            val imageUri = Uri.parse(it)
            selectedImageUri = imageUri
            Glide.with(this)
                .load(imageUri)
                .circleCrop()
                .placeholder(R.drawable.outline_account_circle_24)
                .error(R.drawable.outline_account_circle_24)
                .into(binding.editProfileImageView)
        } ?: run {
            Glide.with(this)
                .load(R.drawable.outline_account_circle_24)
                .circleCrop()
                .into(binding.editProfileImageView)
        }
    }

    private fun saveProfileData() {
        val newName = binding.nameEditText.text.toString().trim()

        if (newName.isEmpty()) {
            binding.nameInputLayout.error = "Name cannot be empty"
            return
        } else {
            binding.nameInputLayout.error = null
        }

        requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit {
            putString("user_name", newName)

            selectedImageUri?.let {
                putString("profile_image_uri", it.toString())
            } ?: run {
                remove("profile_image_uri")
            }

        }

        SnackbarUtils.showCustomSnackbar(requireActivity(), "Profile saved!")
        (activity as? MainActivity)?.loadNavHeaderData()

        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}