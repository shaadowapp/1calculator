package com.shaadow.onecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ChatFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Attach gesture detector only to the left edge
        val leftEdge = view.findViewById<View>(R.id.left_edge_gesture)
        val gestureDetector = android.view.GestureDetector(requireContext(), object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (deltaX < -200 && Math.abs(velocityX) > 800) {
                    // Swipe left: go to Mathly tab (index 1)
                    (requireActivity().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.view_pager)).currentItem = 1
                    return true
                }
                return false
            }
        })
        leftEdge.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }
} 