package dev.eastgate.metamaskclone.utils

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

object ClipboardUtil {
    fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }
}
