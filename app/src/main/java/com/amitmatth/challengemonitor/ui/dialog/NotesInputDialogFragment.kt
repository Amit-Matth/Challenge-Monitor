package com.amitmatth.challengemonitor.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.amitmatth.challengemonitor.databinding.DialogNotesInputBinding
import androidx.core.graphics.drawable.toDrawable

class NotesInputDialogFragment : DialogFragment() {

    private var _binding: DialogNotesInputBinding? = null
    private val binding get() = _binding!!

    private var onSaveClickListener: ((String) -> Unit)? = null
    private var dialogTitle: String? = null
    private var initialNotes: String? = null

    companion object {
        private const val ARG_TITLE = "dialog_title"
        private const val ARG_INITIAL_NOTES = "initial_notes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            dialogTitle = it.getString(ARG_TITLE)
            initialNotes = it.getString(ARG_INITIAL_NOTES)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNotesInputBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialogTitle?.let { binding.dialogTitleTextView.text = it }
        initialNotes?.let { binding.notesEditText.setText(it) }

        binding.saveButton.setOnClickListener {
            val notes = binding.notesEditText.text.toString().trim()
            onSaveClickListener?.invoke(notes)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}