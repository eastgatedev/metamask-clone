package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class BalanceDisplayPanel : JPanel() {
    private val balanceLabel = JBLabel()
    private val usdValueLabel = JBLabel()
    private val refreshButton = JButton("\u21BB") // Refresh symbol

    // Callback for refresh button click
    var onRefreshClick: (() -> Unit)? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()
        border = JBUI.Borders.empty(20, 10)

        // Balance label (large, centered)
        balanceLabel.horizontalAlignment = SwingConstants.CENTER
        balanceLabel.font = balanceLabel.font.deriveFont(Font.BOLD, 28f)

        // USD value label (smaller, centered, gray)
        usdValueLabel.horizontalAlignment = SwingConstants.CENTER
        usdValueLabel.font = usdValueLabel.font.deriveFont(Font.PLAIN, 14f)
        usdValueLabel.foreground = JBColor.gray

        // Refresh button (small, subtle)
        refreshButton.toolTipText = "Refresh balance"
        refreshButton.isBorderPainted = false
        refreshButton.isContentAreaFilled = false
        refreshButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        refreshButton.font = refreshButton.font.deriveFont(14f)
        refreshButton.addActionListener {
            onRefreshClick?.invoke()
        }
        refreshButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                refreshButton.foreground = JBColor.BLUE
            }
            override fun mouseExited(e: MouseEvent) {
                refreshButton.foreground = JBColor.gray
            }
        })
        refreshButton.foreground = JBColor.gray

        // Center panel with balance and refresh button
        val balancePanel = JPanel(FlowLayout(FlowLayout.CENTER, 8, 0))
        balancePanel.isOpaque = false
        balancePanel.add(balanceLabel)
        balancePanel.add(refreshButton)

        val centerPanel = JPanel(BorderLayout())
        centerPanel.isOpaque = false
        centerPanel.add(balancePanel, BorderLayout.CENTER)

        val bottomPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 4))
        bottomPanel.isOpaque = false
        bottomPanel.add(usdValueLabel)

        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        // Default state
        updateBalance("0", "ETH", null)
    }

    /**
     * Show loading state while fetching balance.
     */
    fun showLoading() {
        balanceLabel.text = "..."
        usdValueLabel.text = "Loading balance..."
        usdValueLabel.foreground = JBColor.gray
        refreshButton.isEnabled = false
        revalidate()
        repaint()
    }

    /**
     * Show error state when balance fetch fails.
     */
    fun showError(message: String) {
        balanceLabel.text = "Error"
        usdValueLabel.text = message
        usdValueLabel.foreground = JBColor.RED
        refreshButton.isEnabled = true
        revalidate()
        repaint()
    }

    /**
     * Update balance display with fetched values.
     */
    fun updateBalance(
        balance: String,
        symbol: String,
        usdValue: String?
    ) {
        balanceLabel.text = formatBalance(balance, symbol)
        usdValueLabel.foreground = JBColor.gray // Reset from potential error state
        refreshButton.isEnabled = true

        if (usdValue != null) {
            usdValueLabel.text = usdValue
            usdValueLabel.isVisible = true
        } else {
            usdValueLabel.text = "$0.00"
            usdValueLabel.isVisible = true
        }

        revalidate()
        repaint()
    }

    private fun formatBalance(
        balance: String,
        symbol: String
    ): String {
        val balanceValue = try {
            balance.toBigDecimalOrNull()?.let { value ->
                if (value.scale() > 6) {
                    value.setScale(6, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                } else {
                    value.stripTrailingZeros().toPlainString()
                }
            } ?: "0"
        } catch (e: Exception) {
            "0"
        }

        return "$balanceValue $symbol"
    }
}
