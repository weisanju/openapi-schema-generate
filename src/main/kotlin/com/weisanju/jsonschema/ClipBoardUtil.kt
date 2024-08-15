package com.weisanju.jsonschema

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection


open class ClipboardUtil {

    companion object {
        fun copyToClipboard(text: String) {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(text)
            clipboard.setContents(selection, selection)
        }
    }
}
