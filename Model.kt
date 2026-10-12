package com.igor.teclado

import android.graphics.Color

enum class Type { CHAR, SHIFT, DEL, SYM, ENTER, SPACE, SPACER }

data class Key(
    val type: Type,
    val label: String = "",
    val secondary: String? = null,
    val weight: Float = 1f,
    val special: Boolean = false,
    val state: Int = 0 // shift: 0 off, 1 once, 2 caps lock
)

object Theme {
    val BG = Color.parseColor("#D9DEE9")
    val KEY_TOP = Color.parseColor("#F8F9FB")
    val KEY_BOTTOM = Color.parseColor("#E4E7EE")
    val KEY_SHADOW = Color.parseColor("#B3BAC9")
    val SPECIAL_TOP = Color.parseColor("#CDD3E0")
    val SPECIAL_BOTTOM = Color.parseColor("#BCC3D3")
    val SPECIAL_SHADOW = Color.parseColor("#9AA2B5")
    val ENTER_TOP = Color.parseColor("#E36F4A")
    val ENTER_BOTTOM = Color.parseColor("#D2593A")
    val ENTER_SHADOW = Color.parseColor("#9E3D25")
    val INK = Color.parseColor("#252A36")
    val HINT = Color.parseColor("#6E7587")
}
