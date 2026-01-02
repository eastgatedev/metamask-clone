package dev.eastgate.metamaskclone.ui.panels

import dev.eastgate.metamaskclone.models.Wallet
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel

class ActionButtonPanel : JPanel() {
    private val createButton = JButton("Create Wallet")
    private val importButton = JButton("Import Wallet")
    private val exportButton = JButton("Export Private Key")
    private val deleteButton = JButton("Delete Wallet")

    var onCreateWallet: (() -> Unit)? = null
    var onImportWallet: (() -> Unit)? = null
    var onExportPrivateKey: ((Wallet) -> Unit)? = null
    var onDeleteWallet: ((Wallet) -> Unit)? = null

    private var selectedWallet: Wallet? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = FlowLayout(FlowLayout.CENTER, 8, 8)
        border = BorderFactory.createEmptyBorder(8, 8, 12, 8)
        preferredSize = Dimension(0, 80)
        minimumSize = Dimension(0, 80)

        // Create wallet button
        createButton.preferredSize = Dimension(120, 30)
        createButton.toolTipText = "Create a new wallet"
        createButton.addActionListener { onCreateWallet?.invoke() }
        add(createButton)

        // Import wallet button
        importButton.preferredSize = Dimension(120, 30)
        importButton.toolTipText = "Import an existing wallet"
        importButton.addActionListener { onImportWallet?.invoke() }
        add(importButton)

        // Export private key button
        exportButton.preferredSize = Dimension(130, 30)
        exportButton.toolTipText = "Export private key of selected wallet"
        exportButton.isEnabled = false
        exportButton.addActionListener {
            selectedWallet?.let { wallet ->
                onExportPrivateKey?.invoke(wallet)
            }
        }
        add(exportButton)

        // Delete wallet button
        deleteButton.preferredSize = Dimension(110, 30)
        deleteButton.toolTipText = "Delete the selected wallet"
        deleteButton.foreground = Color.RED
        deleteButton.isEnabled = false
        deleteButton.addActionListener {
            selectedWallet?.let { wallet ->
                onDeleteWallet?.invoke(wallet)
            }
        }
        add(deleteButton)
    }

    fun setSelectedWallet(wallet: Wallet?) {
        selectedWallet = wallet
        exportButton.isEnabled = wallet != null
        deleteButton.isEnabled = wallet != null
    }
}
