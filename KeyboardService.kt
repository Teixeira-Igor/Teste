package com.igor.teclado

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout

class KeyboardService : InputMethodService() {

    private lateinit var root: LinearLayout
    private lateinit var keysBox: LinearLayout
    private var symbols = false
    private var shift = 0 // 0 off, 1 once, 2 caps lock
    private var lastShiftTap = 0L

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.BG)
            setPadding(dp(4), 0, dp(4), dp(6))
        }
        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        bar.addView(IconView(this, IconView.GRID), LinearLayout.LayoutParams(dp(48), dp(36)))
        bar.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        bar.addView(IconView(this, IconView.CHEVRON).apply { setOnClickListener { requestHideSelf(0) } },
            LinearLayout.LayoutParams(dp(56), dp(36)))
        root.addView(bar, LinearLayout.LayoutParams(MATCH_PARENT, dp(40)))

        keysBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keysBox, LinearLayout.LayoutParams(MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        render()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val type = info?.inputType ?: 0
        val cls = type and InputType.TYPE_MASK_CLASS
        symbols = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
        shift = if (cls == InputType.TYPE_CLASS_TEXT &&
            (currentInputConnection?.getCursorCapsMode(type) ?: 0) != 0) 1 else 0
        if (::root.isInitialized) render()
    }

    // ---------- layouts ----------
    private fun chars(l: String, sec: List<String>?) = l.mapIndexed { i, ch ->
        val label = if (shift != 0 && !symbols) ch.uppercaseChar().toString() else ch.toString()
        Key(Type.CHAR, label, sec?.get(i))
    }

    private fun buildRows(): List<List<Key>> {
        val r1: List<Key>
        val r2: List<Key>
        val r3: List<Key>
        val r4: List<Key>
        val sec3 = listOf("*", "\"", "'", ":", ";", "!", "?")
        if (!symbols) {
            r1 = chars("qwertyuiop", "1234567890".map { it.toString() })
            r2 = listOf(Key(Type.SPACER, weight = .5f)) +
                chars("asdfghjkl", listOf("@", "#", "\$", "%", "&", "-", "+", "(", ")")) +
                listOf(Key(Type.SPACER, weight = .5f))
            r3 = listOf(Key(Type.SHIFT, weight = 1.5f, special = true, state = shift)) +
                chars("zxcvbnm", sec3) + Key(Type.DEL, weight = 1.5f, special = true)
            r4 = listOf(Key(Type.SYM, "?123", weight = 1.5f, special = true), Key(Type.CHAR, ","),
                Key(Type.SPACE, "PT · EN", weight = 5f), Key(Type.CHAR, "."), Key(Type.ENTER, weight = 1.5f))
        } else {
            r1 = chars("1234567890", null)
            r2 = chars("@#\$%&-+()/", null)
            r3 = listOf(Key(Type.CHAR, "=", weight = 1.5f, special = true)) +
                sec3.map { Key(Type.CHAR, it) } + Key(Type.DEL, weight = 1.5f, special = true)
            r4 = listOf(Key(Type.SYM, "ABC", weight = 1.5f, special = true), Key(Type.CHAR, ","),
                Key(Type.SPACE, "PT · EN", weight = 5f), Key(Type.CHAR, "."), Key(Type.ENTER, weight = 1.5f))
        }
        return listOf(r1, r2, r3, r4)
    }

    private fun render() {
        keysBox.removeAllViews()
        for (row in buildRows()) {
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in row) {
                val v: View = if (k.type == Type.SPACER) View(this)
                else KeyView(this, k) { key, long -> handle(key, long) }
                line.addView(v, LinearLayout.LayoutParams(0, MATCH_PARENT, k.weight))
            }
            keysBox.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, dp(52)))
        }
    }

    // ---------- input ----------
    private fun handle(k: Key, long: Boolean) {
        val ic = currentInputConnection ?: return
        when (k.type) {
            Type.CHAR -> {
                ic.commitText(if (long) k.secondary ?: k.label else k.label, 1)
                if (shift == 1) { shift = 0; render() }
            }
            Type.SPACE -> { ic.commitText(" ", 1); autoShift(ic) }
            Type.DEL -> {
                if (ic.getSelectedText(0).isNullOrEmpty()) sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                else ic.commitText("", 1)
                autoShift(ic)
            }
            Type.SHIFT -> {
                val now = SystemClock.uptimeMillis()
                shift = when {
                    shift == 2 -> 0
                    now - lastShiftTap < 350 -> 2
                    shift == 1 -> 0
                    else -> 1
                }
                lastShiftTap = now
                render()
            }
            Type.SYM -> { symbols = !symbols; render() }
            Type.ENTER -> { enter(ic); autoShift(ic) }
            Type.SPACER -> {}
        }
    }

    private fun autoShift(ic: InputConnection) {
        if (shift == 2 || symbols) return
        val t = currentInputEditorInfo?.inputType ?: return
        val want = if (ic.getCursorCapsMode(t) != 0) 1 else 0
        if (want != shift) { shift = want; render() }
    }

    private fun enter(ic: InputConnection) {
        val ei = currentInputEditorInfo
        val opts = ei?.imeOptions ?: 0
        val action = opts and EditorInfo.IME_MASK_ACTION
        if (ei == null || (opts and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0 ||
            action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        else ic.performEditorAction(action)
    }
}
