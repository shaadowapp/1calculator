package com.shaadow.onecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VoiceFragment : Fragment() {

    private var _binding: FragmentVoiceBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChatAdapter()
        setupVoiceInput()
        setupTextInput()
        showWelcomeMessage()
    }

    private fun setupChatAdapter() {
        chatAdapter = ChatAdapter()
        binding.chatRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupVoiceInput() {
        binding.btnVoiceInput.setOnClickListener {
            // TODO: Implement voice recognition
            Toast.makeText(requireContext(), "Voice input coming soon!", Toast.LENGTH_SHORT).show()
            
            // Add sample user message
            chatAdapter.addUserMessage("What is the derivative of x²?")
            binding.chatRecycler.smoothScrollToPosition(chatAdapter.itemCount - 1)
            
            // Simulate AI response
            binding.btnVoiceInput.postDelayed({
                chatAdapter.addAIMessage("The derivative of x² is 2x. This is because when you apply the power rule for derivatives, you multiply the exponent by the coefficient and reduce the exponent by 1.")
                binding.chatRecycler.smoothScrollToPosition(chatAdapter.itemCount - 1)
            }, 1000)
        }
    }

    private fun setupTextInput() {
        binding.btnTextInput.setOnClickListener {
            // TODO: Implement text input dialog
            Toast.makeText(requireContext(), "Text input coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showWelcomeMessage() {
        // Show welcome message from AI
        binding.emptyState.visibility = View.GONE
        chatAdapter.addAIMessage("Hello! I'm Mathly, your AI math assistant. I can help you with calculations, problem-solving, and mathematical concepts. How can I assist you today?")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 