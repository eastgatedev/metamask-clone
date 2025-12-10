package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.utils.ClipboardUtil
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class WalletSelectorBar : JPanel() {

    var onWalletClick: (() -> Unit)? = null
    var onCopyAddress: ((String) -> Unit)? = null

    private val walletNameLabel = JBLabel()
    private val walletAddressLabel = JBLabel()
    private val dropdownArrow = JBLabel("\u25BC") // ▼
    private val copyButton = JButton("Copy")

    private var currentWallet: Wallet? = null

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        layout = BorderLayout()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(10, 10)
        )

        // Top row: wallet name with dropdown
        val topRow = JPanel(FlowLayout(FlowLayout.CENTER, 4, 0))
        topRow.isOpaque = false
        topRow.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        walletNameLabel.font = walletNameLabel.font.deriveFont(Font.BOLD, 14f)
        dropdownArrow.font = dropdownArrow.font.deriveFont(Font.PLAIN, 10f)
        dropdownArrow.foreground = JBColor.gray

        topRow.add(walletNameLabel)
        topRow.add(dropdownArrow)

        topRow.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onWalletClick?.invoke()
            }

            override fun mouseEntered(e: MouseEvent) {
                walletNameLabel.foreground = JBColor(0x2196F3.toInt(), 0x42A5F5.toInt())
            }

            override fun mouseExited(e: MouseEvent) {
                walletNameLabel.foreground = JBColor.foreground()
            }
        })

        // Bottom row: address with copy button
        val bottomRow = JPanel(FlowLayout(FlowLayout.CENTER, 8, 0))
        bottomRow.isOpaque = false

        walletAddressLabel.font = walletAddressLabel.font.deriveFont(Font.PLAIN, 12f)
        walletAddressLabel.foreground = JBColor.gray

        copyButton.font = copyButton.font.deriveFont(Font.PLAIN, 10f)
        copyButton.toolTipText = "Copy address to clipboard"
        copyButton.addActionListener {
            currentWallet?.address?.let { address ->
                ClipboardUtil.copyToClipboard(address)
                onCopyAddress?.invoke(address)
            }
        }

        bottomRow.add(walletAddressLabel)
        bottomRow.add(copyButton)

        // Main layout
        val centerPanel = JPanel(BorderLayout())
        centerPanel.isOpaque = false
        centerPanel.add(topRow, BorderLayout.NORTH)
        centerPanel.add(bottomRow, BorderLayout.SOUTH)

        add(centerPanel, BorderLayout.CENTER)

        // Default state
        updateWallet(null)
    }

    private fun setupListeners() {
        // Additional listeners if needed
    }

    fun updateWallet(wallet: Wallet?) {
        currentWallet = wallet

        if (wallet == null) {
            walletNameLabel.text = "No Wallet Selected"
            walletAddressLabel.text = ""
            copyButton.isEnabled = false
        } else {
            walletNameLabel.text = wallet.name
            walletAddressLabel.text = wallet.getShortAddress()
            copyButton.isEnabled = true
        }

        revalidate()
        repaint()
    }

    fun getCurrentWallet(): Wallet? = currentWallet
}
