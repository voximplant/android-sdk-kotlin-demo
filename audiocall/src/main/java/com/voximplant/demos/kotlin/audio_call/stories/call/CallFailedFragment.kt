/*
 * Copyright (c) 2011 - 2021, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.demos.kotlin.audio_call.stories.call

import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.material.snackbar.Snackbar
import com.voximplant.demos.kotlin.audio_call.R
import com.voximplant.demos.kotlin.audio_call.databinding.FragmentCallFailedBinding
import com.voximplant.demos.kotlin.utils.*

class CallFailedFragment : Fragment() {
    private lateinit var binding: FragmentCallFailedBinding
    private val viewModel: CallFailedViewModel by navGraphViewModels(R.id.nav_call_graph)

    private val rootViewGroup: ViewGroup
        get() = activity?.window?.decorView?.rootView as ViewGroup
    private var progressHUDView: ProgressHUDView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallFailedBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.model = viewModel
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reducer = AnimatorInflater.loadAnimator(context, R.animator.reduce_size)
        val increaser = AnimatorInflater.loadAnimator(context, R.animator.regain_size)

        viewModel.setEndpoint(userName = arguments?.getString(ENDPOINT_USERNAME), displayName = arguments?.getString(ENDPOINT_DISPLAY_NAME))

        binding.callFailedStatus.text = arguments?.getString(FAIL_REASON)

        binding.cancel.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        binding.callBackButton.setOnTouchListener { v, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) animate(v, reducer)
            if (motionEvent.action == MotionEvent.ACTION_UP) animate(v, increaser)
            false
        }

        viewModel.showProgress.observe(viewLifecycleOwner) { stringId ->
            showProgressHUD(resources.getString(stringId))
        }

        viewModel.hideProgress.observe(viewLifecycleOwner) {
            hideProgressHUD()
        }

        viewModel.showStringSnackbar.observe(viewLifecycleOwner) { text ->
            Snackbar.make(view, text, Snackbar.LENGTH_LONG).show()
        }

        viewModel.showIntSnackbar.observe(viewLifecycleOwner) { stringId ->
            Snackbar.make(view, getString(stringId), Snackbar.LENGTH_LONG).show()
        }

        viewModel.moveToCall.observe(viewLifecycleOwner) {
            findNavController().navigate(
                R.id.action_callFailedFragment_to_callFragment,
                bundleOf(IS_OUTGOING_CALL to true),
            )
        }

        viewModel.finishActivity.observe(viewLifecycleOwner) {
            activity?.finish()
        }
    }

    private fun animate(view: View, animator: Animator) {
        animator.setTarget(view)
        animator.start()
    }

    private fun showProgressHUD(text: String) {
        progressHUDView?.setText(text)
            ?: run {
                progressHUDView = ProgressHUDView(requireContext())
                progressHUDView?.setText(text)
                rootViewGroup.addView(progressHUDView)
                activity?.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                )
            }
    }

    private fun hideProgressHUD() {
        progressHUDView?.let {
            rootViewGroup.removeView(it)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            progressHUDView = null
        }
    }

}
