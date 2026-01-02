package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingConstants

class ActionButtonsRow : JPanel() {
    var onSendClick: (() -> Unit)? = null
    var onReceiveClick: (() -> Unit)? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = FlowLayout(FlowLayout.CENTER, 30, 10)
        border = JBUI.Borders.empty(10, 10)

        // Send button
        val sendButton = createActionButton("\u2197", "Send") // ↗
        sendButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onSendClick?.invoke()
            }
        })

        // Receive button
        val receiveButton = createActionButton("\u2B07", "Receive") // ⬇
        receiveButton.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onReceiveClick?.invoke()
            }
        })

        add(sendButton)
        add(receiveButton)
    }

    private fun createActionButton(
        icon: String,
        label: String
    ): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        panel.preferredSize = Dimension(70, 70)

        // Circular icon button
        val iconLabel = JBLabel(icon)
        iconLabel.horizontalAlignment = SwingConstants.CENTER
        iconLabel.font = iconLabel.font.deriveFont(Font.BOLD, 18f)
        iconLabel.foreground = JBColor.white
        iconLabel.isOpaque = true
        iconLabel.background = JBColor(0x6366F1.toInt(), 0x818CF8.toInt()) // Purple/indigo
        iconLabel.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        iconLabel.preferredSize = Dimension(48, 48)

        // Label below
        val textLabel = JBLabel(label)
        textLabel.horizontalAlignment = SwingConstants.CENTER
        textLabel.font = textLabel.font.deriveFont(Font.PLAIN, 11f)

        val iconPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))
        iconPanel.isOpaque = false
        iconPanel.add(iconLabel)

        panel.add(iconPanel, BorderLayout.CENTER)
        panel.add(textLabel, BorderLayout.SOUTH)

        // Hover effect
        panel.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                iconLabel.background = JBColor(0x4F46E5.toInt(), 0x6366F1.toInt())
            }

            override fun mouseExited(e: MouseEvent) {
                iconLabel.background = JBColor(0x6366F1.toInt(), 0x818CF8.toInt())
            }
        })

        return panel
    }
}
