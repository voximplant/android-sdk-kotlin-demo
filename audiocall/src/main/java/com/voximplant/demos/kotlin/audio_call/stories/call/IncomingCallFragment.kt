/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.snackbar.Snackbar
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.databinding.FragmentIncomingCallBinding
import com.voximplant.demos.kotlin.audio_call.permissionsHelper
import com.voximplant.demos.kotlin.utils.ACTION_ANSWER_INCOMING_CALL

class IncomingCallFragment : Fragment() {
    private lateinit var binding: FragmentIncomingCallBinding
    private val viewModel: IncomingCallViewModel by navGraphViewModels(R.id.nav_call_graph)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIncomingCallBinding.inflate(layoutInflater)
        binding.model = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reducer = AnimatorInflater.loadAnimator(context, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(context, R.animator.regain_size)

        binding.answerButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.declineButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        permissionsHelper.allPermissionsGranted = { viewModel.answer() }
        permissionsHelper.permissionDenied = { _, openAppSettings ->
            Snackbar.make(binding.root, requireContext().getString(R.string.permission_mic_to_call), Snackbar.LENGTH_LONG).setAction(requireContext().getString(R.string.settings)) { openAppSettings() }.show()
        }

        binding.answerButton.setOnClickListener {
            if (permissionsHelper.allPermissionsGranted()) {
                viewModel.answer()
            } else {
                ActivityCompat.requestPermissions(requireActivity(), permissionsHelper.requiredPermissions, 1)
            }
        }

        binding.declineButton.setOnClickListener {
            viewModel.decline()
        }

        viewModel.moveToCall.observe(viewLifecycleOwner) {
            findNavController().navigate(R.id.action_incomingCallFragment_to_callFragment, arguments)
        }

        viewModel.finishActivity.observe(viewLifecycleOwner) {
            activity?.finish()
        }

        if (arguments?.getBoolean(ACTION_ANSWER_INCOMING_CALL, false) == true) {
            if (permissionsHelper.allPermissionsGranted()) {
                viewModel.answer()
            } else {
                ActivityCompat.requestPermissions(requireActivity(), permissionsHelper.requiredPermissions, 1)
            }
        }

        viewModel.viewCreated()
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }
}