package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ide.BrowserUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.blockchain.EvmTransaction
import dev.eastgate.metamaskclone.core.blockchain.EvmTransactionType
import dev.eastgate.metamaskclone.core.blockchain.TronTransaction
import dev.eastgate.metamaskclone.core.blockchain.TronTransactionType
import dev.eastgate.metamaskclone.core.storage.Network
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

data class ActivityItem(
    val txHash: String,
    val direction: String,
    val counterpartyAddress: String,
    val amount: String,
    val symbol: String,
    val timestamp: Long,
    val isSend: Boolean,
    val explorerUrl: String?
)

class ActivityPanel : JPanel() {
    var onRefreshClick: (() -> Unit)? = null
    var onTransactionClick: ((ActivityItem) -> Unit)? = null

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

    fun updateTransactions(transactions: List<ActivityItem>) {
        transactionListPanel.removeAll()

        if (transactions.isEmpty()) {
            statusLabel.text = "No transactions yet"
            statusLabel.foreground = JBColor.gray
            transactionListPanel.add(statusLabel)
        } else {
            for (item in transactions) {
                transactionListPanel.add(createTransactionRow(item))
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

    private fun createTransactionRow(item: ActivityItem): JPanel {
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
                if (item.explorerUrl != null) {
                    BrowserUtil.browse(item.explorerUrl)
                }
                onTransactionClick?.invoke(item)
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

        // Left side: direction + address
        val leftPanel = JPanel(BorderLayout())
        leftPanel.isOpaque = false

        val directionLabel = JBLabel(item.direction)
        directionLabel.font = directionLabel.font.deriveFont(Font.BOLD, 12f)
        directionLabel.foreground = if (item.isSend) {
            JBColor(0xE53935.toInt(), 0xEF5350.toInt())
        } else {
            JBColor(0x4CAF50.toInt(), 0x66BB6A.toInt())
        }

        val addressLabel = JBLabel(shortenAddress(item.counterpartyAddress))
        addressLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        addressLabel.foreground = JBColor.gray

        leftPanel.add(directionLabel, BorderLayout.NORTH)
        leftPanel.add(addressLabel, BorderLayout.SOUTH)

        // Right side: amount + date
        val rightPanel = JPanel(BorderLayout())
        rightPanel.isOpaque = false

        val sign = if (item.isSend) "-" else "+"
        val amountLabel = JBLabel("$sign${item.amount} ${item.symbol}")
        amountLabel.font = amountLabel.font.deriveFont(Font.BOLD, 12f)
        amountLabel.foreground = if (item.isSend) {
            JBColor(0xE53935.toInt(), 0xEF5350.toInt())
        } else {
            JBColor(0x4CAF50.toInt(), 0x66BB6A.toInt())
        }
        amountLabel.horizontalAlignment = SwingConstants.RIGHT

        val datePanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        datePanel.isOpaque = false

        val dateLabel = JBLabel(dateFormat.format(Date(item.timestamp * 1000)))
        dateLabel.font = dateLabel.font.deriveFont(Font.PLAIN, 10f)
        dateLabel.foreground = JBColor.gray

        datePanel.add(dateLabel)

        rightPanel.add(amountLabel, BorderLayout.NORTH)
        rightPanel.add(datePanel, BorderLayout.SOUTH)

        row.add(leftPanel, BorderLayout.CENTER)
        row.add(rightPanel, BorderLayout.EAST)

        return row
    }

    private fun shortenAddress(address: String): String {
        if (address.length <= 16) return address
        return "${address.take(8)}...${address.takeLast(8)}"
    }
}

// Extension functions to convert chain-specific models to ActivityItem

fun EvmTransaction.toActivityItem(
    walletAddress: String,
    network: Network
): ActivityItem {
    val isSend = from.equals(walletAddress, ignoreCase = true)
    val counterparty = if (isSend) to else from
    val amount = displayValue.toPlainString()
    val symbol = if (transactionType == EvmTransactionType.ERC20) tokenSymbol else network.symbol
    val explorerUrl = network.blockExplorerUrl?.let { "$it/tx/$hash" }

    return ActivityItem(
        txHash = hash,
        direction = if (isSend) "Sent" else "Received",
        counterpartyAddress = counterparty,
        amount = amount,
        symbol = symbol,
        timestamp = timeStamp,
        isSend = isSend,
        explorerUrl = explorerUrl
    )
}

fun TronTransaction.toActivityItem(
    walletAddress: String,
    network: Network
): ActivityItem {
    val isSend = ownerAddress == walletAddress
    val counterparty = if (isSend) toAddress else ownerAddress
    val amount = when (transactionType) {
        TronTransactionType.TRC20 -> displayValue?.toPlainString() ?: "0"
        TronTransactionType.TRX -> amountInTrx?.toPlainString() ?: "0"
    }
    val symbol = if (transactionType == TronTransactionType.TRC20) {
        tokenSymbol ?: ""
    } else {
        network.symbol
    }
    val explorerUrl = network.blockExplorerUrl?.let { "$it/#/transaction/$txID" }

    return ActivityItem(
        txHash = txID,
        direction = if (isSend) "Sent" else "Received",
        counterpartyAddress = counterparty,
        amount = amount,
        symbol = symbol,
        timestamp = blockTimestamp / 1000,
        isSend = isSend,
        explorerUrl = explorerUrl
    )
}
