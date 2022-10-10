/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.os.Bundle
import android.telecom.CallAudioState
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.databinding.FragmentOngoingCallBinding
import com.voximplant.demos.kotlin.utils.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class OngoingCallFragment : Fragment() {
    private lateinit var binding: FragmentOngoingCallBinding
    private val viewModel: OngoingCallViewModel by navGraphViewModels(R.id.nav_call_graph)

    private var shouldClearTextView = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOngoingCallBinding.inflate(layoutInflater)
        binding.model = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.keyPadView.model = viewModel

        val reducer = AnimatorInflater.loadAnimator(context, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(context, R.animator.regain_size)

        viewModel.onHideKeypadPressed.observe(viewLifecycleOwner, {
            shouldClearTextView = true
            showKeypad(false)
        })

        lifecycleScope.launchWhenCreated {
            viewModel.callAudioState.collect { state ->
                when (state?.route) {
                    CallAudioState.ROUTE_EARPIECE -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_internal)
                    CallAudioState.ROUTE_SPEAKER -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_external)
                    CallAudioState.ROUTE_WIRED_HEADSET -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_headphones)
                    CallAudioState.ROUTE_BLUETOOTH -> binding.audioButtonIcon.setImageResource(R.drawable.ic_bluetooth)
                    else -> binding.audioButtonIcon.setImageResource(R.drawable.ic_audio_disabled)
                }
            }
        }

        binding.muteButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.audioButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.holdButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.hangupButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.muteButton.setOnClickListener {
            viewModel.mute()
        }

        binding.audioButton.setOnClickListener {
            showAudioDeviceSelectionDialog(viewModel.callAudioState)
        }

        binding.holdButton.setOnClickListener {
            viewModel.hold()
        }

        binding.hangupButton.setOnClickListener {
            viewModel.hangup()
        }

        binding.keypadButton.setOnClickListener {
            showKeypad(true)
        }

        binding.keyPadView.keypadHideButton.setOnClickListener {
            showKeypad(false)
        }

        viewModel.charDTMF.observe(viewLifecycleOwner, { symbol ->
            if (shouldClearTextView) {
                binding.callerNameTextView.text = symbol
            } else {
                binding.callerNameTextView.text =
                    if (binding.callerNameTextView.text.length < 15) binding.callerNameTextView.text
                        .toString() + symbol else binding.callerNameTextView.text.toString()
                        .substring(1) + symbol
            }
            shouldClearTextView = false
        })

        viewModel.enableButtons.observe(viewLifecycleOwner, { enabled ->
            val alpha = if (enabled) 1.0 else 0.25
            binding.holdButton.alpha = alpha.toFloat()
            binding.keypadButton.alpha = alpha.toFloat()
            binding.holdButton.isEnabled = enabled
            binding.keypadButton.isEnabled = enabled
        })

        viewModel.enableKeypad.observe(viewLifecycleOwner, { enabled ->
            val alpha = if (enabled) 1.0 else 0.25
            binding.keypadButton.alpha = alpha.toFloat()
            binding.keypadButton.isEnabled = enabled
        })

        viewModel.muted.observe(viewLifecycleOwner, { muted ->
            if (muted) {
                binding.muteButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorRed
                    )
                )
                binding.muteButtonIcon.setImageResource(R.drawable.ic_micoff)
                binding.muteValue = getString(R.string.unmute)
            } else {
                binding.muteButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.call_option_default_back
                    )
                )
                binding.muteButtonIcon.setImageResource(R.drawable.ic_micon)
                binding.muteValue = getString(R.string.mute)
            }
        })

        viewModel.onHold.observe(viewLifecycleOwner, { onHold ->
            binding.holdButton.isEnabled = true
            if (onHold) {
                binding.holdButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.colorRed
                    )
                )
                binding.holdValue = getString(R.string.resume)
            } else {
                binding.holdButton.setCardBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.call_option_default_back
                    )
                )
                binding.holdValue = getString(R.string.hold)
            }
        })

        viewModel.finishActivity.observe(viewLifecycleOwner, {
            activity?.finish()
        })

        viewModel.moveToCallFailed.observe(viewLifecycleOwner, { reason ->
            findNavController().navigate(
                R.id.action_callFragment_to_callFailedFragment,
                bundleOf(
                    ENDPOINT_USERNAME to viewModel.userName.value,
                    ENDPOINT_DISPLAY_NAME to viewModel.displayName.value,
                    FAIL_REASON to reason,
                )
            )
        })

        arguments?.let {
            viewModel.onCreateWithCall(
                it.getBoolean(IS_ONGOING_CALL, false),
                it.getBoolean(IS_OUTGOING_CALL, false),
                it.getBoolean(IS_INCOMING_CALL, false),
            )
        }

        activity?.onBackPressedDispatcher?.addCallback(this) { }
    }


    private fun showAudioDeviceSelectionDialog(callAudioState: StateFlow<CallAudioState?>?) {
        lifecycleScope.launchWhenCreated {
            callAudioState?.collect { state ->
                val routes = state?.supportedRouteMask?.let { CallAudioState.audioRouteToString(it) }?.split(", ")
                AlertDialog.Builder(requireContext()).setTitle(R.string.alert_select_audio_device)
                    .setItems(routes?.toTypedArray()?.map {
                        if (CallAudioState.audioRouteToString(state.route) == it) {
                            "$it (Current)"
                        } else {
                            it
                        }
                    }?.toTypedArray()) { _, which ->
                        routes?.get(which)?.let {
                            viewModel.selectAudioDevice(it)
                        }
                    }
                    .create()
                    .show()
            }
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    private fun showKeypad(show: Boolean) {
        binding.muteButton.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.keypadButton.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.audioButton.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.holdButton.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.muteLabel.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.keypadLabel.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.audioLabel.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.holdLabel.visibility = if (show) View.INVISIBLE else View.VISIBLE
        binding.keyPadView.keypadView.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }
}