package dev.eastgate.metamaskclone.ui.panels

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.utils.ClipboardUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.Timer

class BitcoinAddressesPanel : JPanel() {
    var onGenerateNewAddress: (() -> Unit)? = null
    var onAddressCopied: ((String) -> Unit)? = null

    private val addressListPanel = JPanel()
    private val statusLabel = JBLabel("Loading addresses...")
    private val generateButton = JButton("+ New Address")
    private val subtitleLabel = JBLabel("Receiving Addresses")
    private var currentAddresses: List<String> = emptyList()

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(8, 10)
        )

        // Header row
        val headerRow = JPanel(BorderLayout())
        headerRow.isOpaque = false
        headerRow.border = JBUI.Borders.emptyBottom(6)

        val titlePanel = JPanel(BorderLayout())
        titlePanel.isOpaque = false

        val titleLabel = JBLabel("Bitcoin Core Wallet")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 14f)

        subtitleLabel.font = subtitleLabel.font.deriveFont(Font.PLAIN, 11f)
        subtitleLabel.foreground = JBColor.gray

        titlePanel.add(titleLabel, BorderLayout.NORTH)
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH)

        generateButton.font = generateButton.font.deriveFont(Font.PLAIN, 11f)
        generateButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        generateButton.addActionListener { onGenerateNewAddress?.invoke() }

        headerRow.add(titlePanel, BorderLayout.WEST)
        headerRow.add(generateButton, BorderLayout.EAST)

        // Address list
        addressListPanel.layout = BoxLayout(addressListPanel, BoxLayout.Y_AXIS)
        addressListPanel.isOpaque = false

        statusLabel.horizontalAlignment = SwingConstants.CENTER
        statusLabel.foreground = JBColor.gray
        statusLabel.border = JBUI.Borders.empty(10, 0)

        val scrollPane = JBScrollPane(addressListPanel)
        scrollPane.border = JBUI.Borders.empty()
        scrollPane.preferredSize = Dimension(0, 100)

        add(headerRow, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)

        showLoading()
    }

    fun getFirstAddress(): String? = currentAddresses.firstOrNull()

    fun updateAddresses(addresses: List<String>) {
        currentAddresses = addresses
        subtitleLabel.text = "Receiving Addresses (${addresses.size})"
        addressListPanel.removeAll()

        if (addresses.isEmpty()) {
            statusLabel.text = "No addresses yet. Generate one to get started."
            statusLabel.foreground = JBColor.gray
            addressListPanel.add(statusLabel)
        } else {
            for (address in addresses) {
                addressListPanel.add(createAddressRow(address))
            }
        }

        addressListPanel.revalidate()
        addressListPanel.repaint()
    }

    fun showLoading() {
        addressListPanel.removeAll()
        statusLabel.text = "Loading addresses..."
        statusLabel.foreground = JBColor.gray
        addressListPanel.add(statusLabel)
        addressListPanel.revalidate()
        addressListPanel.repaint()
    }

    fun showError(message: String) {
        addressListPanel.removeAll()
        statusLabel.text = message
        statusLabel.foreground = JBColor(0xE53935.toInt(), 0xEF5350.toInt())
        addressListPanel.add(statusLabel)
        addressListPanel.revalidate()
        addressListPanel.repaint()
    }

    private fun createAddressRow(address: String): JPanel {
        val row = JPanel(BorderLayout())
        row.isOpaque = false
        row.border = JBUI.Borders.empty(2, 0)
        row.maximumSize = Dimension(Int.MAX_VALUE, 28)

        val shortAddress = shortenAddress(address)
        val addressLabel = JBLabel(shortAddress)
        addressLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
        addressLabel.foreground = JBColor.foreground()
        addressLabel.toolTipText = address

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        buttonPanel.isOpaque = false

        val qrBtn = JButton("QR")
        qrBtn.font = qrBtn.font.deriveFont(Font.PLAIN, 10f)
        qrBtn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        qrBtn.toolTipText = "Show QR Code"
        qrBtn.addActionListener {
            showAddressQRDialog(address)
        }

        val copyBtn = JButton("Copy")
        copyBtn.font = copyBtn.font.deriveFont(Font.PLAIN, 10f)
        copyBtn.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        copyBtn.addActionListener {
            ClipboardUtil.copyToClipboard(address)
            onAddressCopied?.invoke(address)
        }

        buttonPanel.add(qrBtn)
        buttonPanel.add(copyBtn)

        row.add(addressLabel, BorderLayout.CENTER)
        row.add(buttonPanel, BorderLayout.EAST)

        return row
    }

    private fun shortenAddress(address: String): String {
        if (address.length <= 20) return address
        return "${address.take(10)}...${address.takeLast(8)}"
    }

    fun showAddressQRDialog(address: String) {
        val dialog = BitcoinAddressQRDialog(address)
        dialog.show()
    }

    /**
     * Dialog showing a Bitcoin address with its QR code.
     */
    class BitcoinAddressQRDialog(
        private val address: String
    ) : DialogWrapper(true) {
        init {
            title = "Bitcoin Address"
            init()
        }

        override fun createCenterPanel(): javax.swing.JComponent {
            val panel = JPanel(BorderLayout())
            panel.border = JBUI.Borders.empty(15)
            panel.preferredSize = Dimension(320, 360)

            // QR Code
            val qrPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            try {
                val qrImage = generateQRCode(address, 180, 180)
                val imageLabel = object : javax.swing.JLabel() {
                    override fun paintComponent(g: Graphics) {
                        super.paintComponent(g)
                        g.drawImage(qrImage, 0, 0, null)
                    }
                }
                imageLabel.preferredSize = Dimension(180, 180)
                qrPanel.add(imageLabel)
            } catch (e: Exception) {
                val errorLabel = JBLabel("QR Code unavailable")
                errorLabel.preferredSize = Dimension(180, 180)
                errorLabel.horizontalAlignment = SwingConstants.CENTER
                errorLabel.border = BorderFactory.createLineBorder(JBColor.border())
                qrPanel.add(errorLabel)
            }

            // Address display
            val addressLabel = JBLabel("<html><center>$address</center></html>")
            addressLabel.font = Font(Font.MONOSPACED, Font.PLAIN, 11)
            addressLabel.horizontalAlignment = SwingConstants.CENTER
            addressLabel.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1, true),
                JBUI.Borders.empty(8)
            )

            // Copy button
            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            val copyButton = JButton("Copy Address")
            copyButton.addActionListener {
                ClipboardUtil.copyToClipboard(address)
                copyButton.text = "Copied!"
                Timer(2000) { copyButton.text = "Copy Address" }.apply {
                    isRepeats = false
                    start()
                }
            }
            buttonPanel.add(copyButton)

            val centerPanel = JPanel(BorderLayout())
            centerPanel.add(qrPanel, BorderLayout.CENTER)
            centerPanel.add(addressLabel, BorderLayout.SOUTH)

            panel.add(centerPanel, BorderLayout.CENTER)
            panel.add(buttonPanel, BorderLayout.SOUTH)

            return panel
        }

        private fun generateQRCode(
            content: String,
            width: Int,
            height: Int
        ): BufferedImage {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height)
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    image.setRGB(x, y, if (bitMatrix[x, y]) Color.BLACK.rgb else Color.WHITE.rgb)
                }
            }
            return image
        }

        override fun createActions(): Array<Action> {
            return arrayOf(cancelAction)
        }
    }
}
