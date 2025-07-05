package com.shaadow.onecalculator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class FragmentVoiceBinding private constructor(
    private val rootView: View,
    val btnVoiceInput: MaterialButton,
    val btnTextInput: MaterialButton,
    val chatRecycler: RecyclerView,
    val emptyState: View
) {
    val root: View = rootView

    companion object {
        fun inflate(
            inflater: LayoutInflater,
            parent: ViewGroup?,
            attachToParent: Boolean
        ): FragmentVoiceBinding {
            val root = inflater.inflate(R.layout.fragment_voice, parent, attachToParent)
            return FragmentVoiceBinding(
                rootView = root,
                btnVoiceInput = root.findViewById(R.id.btn_voice_input),
                btnTextInput = root.findViewById(R.id.btn_text_input),
                chatRecycler = root.findViewById(R.id.chat_recycler),
                emptyState = root.findViewById(R.id.empty_state)
            )
        }
    }
} 