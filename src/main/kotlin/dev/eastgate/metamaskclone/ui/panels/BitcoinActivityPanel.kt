package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.blockchain.BitcoinTransaction
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

class BitcoinActivityPanel : JPanel() {
    var onRefreshClick: (() -> Unit)? = null
    var onTransactionClick: ((BitcoinTransaction) -> Unit)? = null

    private val transactionListPanel = JPanel()
    private val statusLabel = JBLabel("Loading transactions...")
    private val refreshButton = JButton("Refresh")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()

        // Header
        val headerRow = JPanel(BorderLayout())
        headerRow.isOpaque = false
        headerRow.border = JBUI.Borders.empty(8, 10)

        val titleLabel = JBLabel("Transaction History")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 13f)

        refreshButton.font = refreshButton.font.deriveFont(Font.PLAIN, 11f)
        refreshButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        refreshButton.addActionListener { onRefreshClick?.invoke() }

        headerRow.add(titleLabel, BorderLayout.WEST)
        headerRow.add(refreshButton, BorderLayout.EAST)

        // Transaction list
        transactionListPanel.layout = BoxLayout(transactionListPanel, BoxLayout.Y_AXIS)
        transactionListPanel.isOpaque = false

        statusLabel.horizontalAlignment = SwingConstants.CENTER
        statusLabel.foreground = JBColor.gray
        statusLabel.border = JBUI.Borders.empty(20, 0)

        val scrollPane = JBScrollPane(transactionListPanel)
        scrollPane.border = JBUI.Borders.empty()

        add(headerRow, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        showLoading()
    }

    fun updateTransactions(transactions: List<BitcoinTransaction>) {
        transactionListPanel.removeAll()

        if (transactions.isEmpty()) {
            statusLabel.text = "No transactions yet"
            statusLabel.foreground = JBColor.gray
            transactionListPanel.add(statusLabel)
        } else {
            // Show most recent first
            for (tx in transactions.reversed()) {
                transactionListPanel.add(createTransactionRow(tx))
            }
        }

        transactionListPanel.revalidate()
        transactionListPanel.repaint()
    }

    fun showLoading() {
        transactionListPanel.removeAll()
        statusLabel.text = "Loading transactions..."
        statusLabel.foreground = JBColor.gray
        transactionListPanel.add(statusLabel)
        transactionListPanel.revalidate()
        transactionListPanel.repaint()
    }

    fun showError(message: String) {
        transactionListPanel.removeAll()
        statusLabel.text = message
        statusLabel.foreground = JBColor(0xE53935.toInt(), 0xEF5350.toInt())
        transactionListPanel.add(statusLabel)
        transactionListPanel.revalidate()
        transactionListPanel.repaint()
    }

    private fun createTransactionRow(tx: BitcoinTransaction): JPanel {
        val row = JPanel(BorderLayout())
        row.isOpaque = false
        row.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(8, 10)
        )
        row.maximumSize = java.awt.Dimension(Int.MAX_VALUE, 65)
        row.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        row.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onTransactionClick?.invoke(tx)
            }

            override fun mouseEntered(e: MouseEvent) {
                row.background = JBColor(0xF5F5F5.toInt(), 0x3C3F41.toInt())
                row.isOpaque = true
                row.repaint()
            }

            override fun mouseExited(e: MouseEvent) {
                row.isOpaque = false
                row.repaint()
            }
        })

        // Left side: category + address
        val leftPanel = JPanel(BorderLayout())
        leftPanel.isOpaque = false

        val categoryLabel = JBLabel(formatCategory(tx.category))
        categoryLabel.font = categoryLabel.font.deriveFont(Font.BOLD, 12f)
        categoryLabel.foreground = getCategoryColor(tx.category)

        val addressLabel = JBLabel(shortenAddress(tx.address))
        addressLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        addressLabel.foreground = JBColor.gray

        leftPanel.add(categoryLabel, BorderLayout.NORTH)
        leftPanel.add(addressLabel, BorderLayout.SOUTH)

        // Right side: amount + date
        val rightPanel = JPanel(BorderLayout())
        rightPanel.isOpaque = false

        val sign = if (tx.amount >= 0) "+" else ""
        val amountLabel = JBLabel("$sign${String.format("%.8f", tx.amount)} BTC")
        amountLabel.font = amountLabel.font.deriveFont(Font.BOLD, 12f)
        amountLabel.foreground = if (tx.amount >= 0) {
            JBColor(0x4CAF50.toInt(), 0x66BB6A.toInt())
        } else {
            JBColor(0xE53935.toInt(), 0xEF5350.toInt())
        }
        amountLabel.horizontalAlignment = SwingConstants.RIGHT

        val datePanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        datePanel.isOpaque = false

        val dateLabel = JBLabel(dateFormat.format(Date(tx.time * 1000)))
        dateLabel.font = dateLabel.font.deriveFont(Font.PLAIN, 10f)
        dateLabel.foreground = JBColor.gray

        val confLabel = JBLabel("${tx.confirmations} conf")
        confLabel.font = confLabel.font.deriveFont(Font.PLAIN, 10f)
        confLabel.foreground = JBColor.gray

        datePanel.add(dateLabel)
        datePanel.add(confLabel)

        rightPanel.add(amountLabel, BorderLayout.NORTH)
        rightPanel.add(datePanel, BorderLayout.SOUTH)

        row.add(leftPanel, BorderLayout.CENTER)
        row.add(rightPanel, BorderLayout.EAST)

        return row
    }

    private fun formatCategory(category: String): String {
        return when (category) {
            "receive" -> "Received"
            "send" -> "Sent"
            "generate" -> "Mined"
            "immature" -> "Immature"
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getCategoryColor(category: String): java.awt.Color {
        return when (category) {
            "receive", "generate" -> JBColor(0x4CAF50.toInt(), 0x66BB6A.toInt())
            "send" -> JBColor(0xE53935.toInt(), 0xEF5350.toInt())
            else -> JBColor.foreground()
        }
    }

    private fun shortenAddress(address: String): String {
        if (address.length <= 16) return address
        return "${address.take(8)}...${address.takeLast(8)}"
    }
}
