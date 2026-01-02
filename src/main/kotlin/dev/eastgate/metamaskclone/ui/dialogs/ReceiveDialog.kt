package dev.eastgate.metamaskclone.ui.dialogs

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.utils.ClipboardUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.*

class ReceiveDialog(
    private val project: Project,
    private val wallet: Wallet
) : DialogWrapper(project) {
    init {
        title = "Receive"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(20)
        panel.preferredSize = Dimension(350, 400)

        // Title
        val titleLabel = JBLabel("Your Address")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.horizontalAlignment = SwingConstants.CENTER
        titleLabel.border = JBUI.Borders.emptyBottom(10)

        // QR Code
        val qrCodePanel = createQRCodePanel()

        // Address display
        val addressPanel = JPanel(BorderLayout())
        addressPanel.border = JBUI.Borders.empty(15, 0)

        val addressLabel = JBLabel(wallet.address)
        addressLabel.font = addressLabel.font.deriveFont(Font.PLAIN, 11f)
        addressLabel.horizontalAlignment = SwingConstants.CENTER
        addressLabel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(10)
        )

        addressPanel.add(addressLabel, BorderLayout.CENTER)

        // Copy button
        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        val copyButton = JButton("Copy Address")
        copyButton.addActionListener {
            ClipboardUtil.copyToClipboard(wallet.address)
            copyButton.text = "Copied!"
            Timer(2000) { copyButton.text = "Copy Address" }.apply {
                isRepeats = false
                start()
            }
        }
        buttonPanel.add(copyButton)

        // Warning
        val warningLabel =
            JBLabel("<html><center><small>Only send tokens on the same network.<br/>Sending to wrong network may result in loss.</small></center></html>")
        warningLabel.horizontalAlignment = SwingConstants.CENTER
        warningLabel.foreground = JBColor(0xFF9800.toInt(), 0xFFB74D.toInt())
        warningLabel.border = JBUI.Borders.emptyTop(15)

        // Assemble
        val centerContent = JPanel(BorderLayout())
        centerContent.add(qrCodePanel, BorderLayout.CENTER)
        centerContent.add(addressPanel, BorderLayout.SOUTH)

        panel.add(titleLabel, BorderLayout.NORTH)
        panel.add(centerContent, BorderLayout.CENTER)

        val southPanel = JPanel(BorderLayout())
        southPanel.add(buttonPanel, BorderLayout.NORTH)
        southPanel.add(warningLabel, BorderLayout.SOUTH)
        panel.add(southPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun createQRCodePanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER))

        try {
            val qrImage = generateQRCode(wallet.address, 180, 180)
            val imageLabel = object : JLabel() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    g.drawImage(qrImage, 0, 0, null)
                }
            }
            imageLabel.preferredSize = Dimension(180, 180)
            panel.add(imageLabel)
        } catch (e: Exception) {
            // Fallback if QR generation fails
            val errorLabel = JBLabel("QR Code unavailable")
            errorLabel.preferredSize = Dimension(180, 180)
            errorLabel.horizontalAlignment = SwingConstants.CENTER
            errorLabel.border = BorderFactory.createLineBorder(JBColor.border())
            panel.add(errorLabel)
        }

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
