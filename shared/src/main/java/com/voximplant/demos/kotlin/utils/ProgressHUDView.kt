package com.voximplant.demos.kotlin.utils

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.voximplant.demos.kotlin.utils.databinding.ProgressHudLayoutBinding

class ProgressHUDView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {
    private var binding: ProgressHudLayoutBinding

    init {
        inflate(R.layout.progress_hud_layout, true)
        binding = ProgressHudLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setText(text: String) {
        binding.loadingTextLabel.text = text
    }
}
