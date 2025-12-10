package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.SwingConstants

class BalanceDisplayPanel : JPanel() {

    private val balanceLabel = JBLabel()
    private val usdValueLabel = JBLabel()

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

        // Center panel
        val centerPanel = JPanel(BorderLayout())
        centerPanel.isOpaque = false
        centerPanel.add(balanceLabel, BorderLayout.CENTER)

        val bottomPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 4))
        bottomPanel.isOpaque = false
        bottomPanel.add(usdValueLabel)

        add(centerPanel, BorderLayout.CENTER)
        add(bottomPanel, BorderLayout.SOUTH)

        // Default state
        updateBalance("0", "ETH", null)
    }

    fun updateBalance(balance: String, symbol: String, usdValue: String?) {
        balanceLabel.text = formatBalance(balance, symbol)

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

    private fun formatBalance(balance: String, symbol: String): String {
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
