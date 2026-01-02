package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.storage.Network
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel

class NetworkSelectorBar : JPanel() {
    var onNetworkClick: (() -> Unit)? = null

    private val networkIconLabel = JBLabel()
    private val networkNameLabel = JBLabel()
    private val dropdownArrow = JBLabel("\u25BC") // ▼
    private val testnetBadge = JBLabel("Testnet")

    private var currentNetwork: Network? = null

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        layout = BorderLayout()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(8, 10)
        )
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        // Left side: network icon
        networkIconLabel.font = networkIconLabel.font.deriveFont(Font.BOLD, 14f)
        networkIconLabel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(4, 8)
        )

        // Center: network name and testnet badge
        val centerPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        centerPanel.isOpaque = false

        networkNameLabel.font = networkNameLabel.font.deriveFont(Font.PLAIN, 13f)

        testnetBadge.font = testnetBadge.font.deriveFont(Font.PLAIN, 10f)
        testnetBadge.foreground = JBColor(0xFF9800.toInt(), 0xFFB74D.toInt())
        testnetBadge.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(0xFF9800.toInt(), 0xFFB74D.toInt()), 1, true),
            JBUI.Borders.empty(2, 6)
        )
        testnetBadge.isVisible = false

        centerPanel.add(networkNameLabel)
        centerPanel.add(testnetBadge)

        // Right side: dropdown arrow
        dropdownArrow.font = dropdownArrow.font.deriveFont(Font.PLAIN, 10f)
        dropdownArrow.foreground = JBColor.gray

        // Add to main panel
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        leftPanel.isOpaque = false
        leftPanel.add(networkIconLabel)

        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        rightPanel.isOpaque = false
        rightPanel.add(dropdownArrow)

        add(leftPanel, BorderLayout.WEST)
        add(centerPanel, BorderLayout.CENTER)
        add(rightPanel, BorderLayout.EAST)

        // Default state
        updateNetwork(null)
    }

    private fun setupListeners() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onNetworkClick?.invoke()
            }

            override fun mouseEntered(e: MouseEvent) {
                background = JBColor(0xF5F5F5.toInt(), 0x3C3F41.toInt())
            }

            override fun mouseExited(e: MouseEvent) {
                background = null
            }
        })
    }

    fun updateNetwork(network: Network?) {
        currentNetwork = network

        if (network == null) {
            networkIconLabel.text = "?"
            networkNameLabel.text = "Select Network"
            testnetBadge.isVisible = false
        } else {
            networkIconLabel.text = getNetworkInitial(network)
            networkNameLabel.text = network.name
            testnetBadge.isVisible = network.isTestnet
        }

        revalidate()
        repaint()
    }

    private fun getNetworkInitial(network: Network): String {
        return when {
            network.symbol.equals("ETH", ignoreCase = true) -> "E"
            network.symbol.equals("BNB", ignoreCase = true) ||
                network.symbol.equals("tBNB", ignoreCase = true) -> "B"
            network.symbol.equals("MATIC", ignoreCase = true) -> "P"
            else -> network.symbol.firstOrNull()?.uppercase() ?: "?"
        }
    }

    fun getCurrentNetwork(): Network? = currentNetwork
}
