package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.blockchain.BlockchainService
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.models.BlockchainType
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class NetworkSelectionDialog(
    private val project: Project,
    private val networkManager: NetworkManager
) : DialogWrapper(project) {
    private var selectedNetwork: Network? = null
    private val contentPanel = JPanel()

    init {
        title = "Select a network"
        init()
    }

    override fun createCenterPanel(): JComponent {
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.border = JBUI.Borders.empty(10)

        refreshNetworkList()

        val scrollPane = JBScrollPane(contentPanel)
        scrollPane.preferredSize = Dimension(350, 450)
        scrollPane.border = null

        return scrollPane
    }

    private fun refreshNetworkList() {
        contentPanel.removeAll()

        // Enabled networks section
        val enabledNetworks = networkManager.getEnabledNetworks()
        if (enabledNetworks.isNotEmpty()) {
            contentPanel.add(createSectionHeader("Enabled networks"))
            contentPanel.add(Box.createVerticalStrut(8))

            for (network in enabledNetworks) {
                contentPanel.add(createNetworkRow(network, isEnabled = true))
                contentPanel.add(Box.createVerticalStrut(4))
            }
        }

        // Additional networks section
        val disabledNetworks = networkManager.getDisabledPredefinedNetworks()
        if (disabledNetworks.isNotEmpty()) {
            contentPanel.add(Box.createVerticalStrut(16))
            contentPanel.add(createSectionHeader("Additional networks"))
            contentPanel.add(Box.createVerticalStrut(8))

            for (network in disabledNetworks) {
                contentPanel.add(createNetworkRow(network, isEnabled = false))
                contentPanel.add(Box.createVerticalStrut(4))
            }
        }

        // Add custom network button
        contentPanel.add(Box.createVerticalStrut(16))
        contentPanel.add(createAddCustomNetworkButton())

        contentPanel.add(Box.createVerticalGlue())

        contentPanel.revalidate()
        contentPanel.repaint()
    }

    private fun createSectionHeader(text: String): JComponent {
        val label = JBLabel(text)
        label.font = label.font.deriveFont(Font.PLAIN, 12f)
        label.foreground = JBColor.gray
        label.alignmentX = Component.LEFT_ALIGNMENT
        return label
    }

    private fun createNetworkRow(
        network: Network,
        isEnabled: Boolean
    ): JComponent {
        val row = JPanel(BorderLayout())
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, 50)
        row.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(8, 12)
        )
        row.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        val isSelected = networkManager.selectedNetwork.value.id == network.id

        if (isSelected) {
            row.background = JBColor(0xE3F2FD.toInt(), 0x2D4F6E.toInt())
            row.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor(0x2196F3.toInt(), 0x42A5F5.toInt()), 2, true),
                JBUI.Borders.empty(7, 11)
            )
        }

        // Left side: icon and name
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        leftPanel.isOpaque = false

        val iconLabel = JBLabel(getNetworkInitial(network))
        iconLabel.font = iconLabel.font.deriveFont(Font.BOLD, 12f)
        iconLabel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(4, 8)
        )
        leftPanel.add(iconLabel)

        val namePanel = JPanel()
        namePanel.layout = BoxLayout(namePanel, BoxLayout.Y_AXIS)
        namePanel.isOpaque = false

        val nameLabel = JBLabel(network.name)
        nameLabel.font = nameLabel.font.deriveFont(Font.PLAIN, 13f)
        namePanel.add(nameLabel)

        if (network.isTestnet) {
            val testnetLabel = JBLabel("Testnet")
            testnetLabel.font = testnetLabel.font.deriveFont(Font.PLAIN, 10f)
            testnetLabel.foreground = JBColor(0xFF9800.toInt(), 0xFFB74D.toInt())
            namePanel.add(testnetLabel)
        }

        leftPanel.add(namePanel)

        // Right side: action button or checkmark
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        rightPanel.isOpaque = false

        if (isEnabled) {
            if (isSelected) {
                val checkLabel = JBLabel("\u2713") // ✓
                checkLabel.font = checkLabel.font.deriveFont(Font.BOLD, 16f)
                checkLabel.foreground = JBColor(0x4CAF50.toInt(), 0x81C784.toInt())
                rightPanel.add(checkLabel)
            }

            // Three-dot menu for enabled networks
            val menuButton = JBLabel("\u22EE") // ⋮
            menuButton.font = menuButton.font.deriveFont(Font.BOLD, 16f)
            menuButton.foreground = JBColor.gray
            menuButton.border = JBUI.Borders.empty(0, 8)
            menuButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            menuButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    e.consume()
                    showNetworkMenu(network, menuButton)
                }
            })
            rightPanel.add(menuButton)
        } else {
            // Add button for disabled networks
            val addButton = JButton("Add")
            addButton.font = addButton.font.deriveFont(Font.PLAIN, 11f)
            addButton.addActionListener {
                networkManager.enableNetwork(network.id)
                // For Bitcoin networks, immediately prompt RPC config
                if (network.blockchainType == BlockchainType.BITCOIN) {
                    val configDialog = BitcoinRpcConfigDialog(project, networkManager, network)
                    if (configDialog.showAndGet()) {
                        BlockchainService.getInstance(project).invalidateNetwork(network.id)
                    }
                }
                refreshNetworkList()
            }
            rightPanel.add(addButton)
        }

        row.add(leftPanel, BorderLayout.WEST)
        row.add(rightPanel, BorderLayout.EAST)

        // Click to select (only for enabled networks)
        if (isEnabled) {
            row.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 1) {
                        selectedNetwork = network
                        networkManager.selectNetwork(network.id)
                        close(OK_EXIT_CODE)
                    }
                }

                override fun mouseEntered(e: MouseEvent) {
                    if (!isSelected) {
                        row.background = JBColor(0xF5F5F5.toInt(), 0x3C3F41.toInt())
                    }
                }

                override fun mouseExited(e: MouseEvent) {
                    if (!isSelected) {
                        row.background = null
                    }
                }
            })
        }

        return row
    }

    private fun showNetworkMenu(
        network: Network,
        component: JComponent
    ) {
        val popup = JPopupMenu()

        if (networkManager.isCustomNetwork(network.id)) {
            val editItem = JMenuItem("Edit")
            editItem.addActionListener {
                val editDialog = AddCustomNetworkDialog(project, networkManager, network)
                if (editDialog.showAndGet()) {
                    refreshNetworkList()
                }
            }
            popup.add(editItem)

            val removeItem = JMenuItem("Remove")
            removeItem.addActionListener {
                networkManager.removeCustomNetwork(network.id)
                refreshNetworkList()
            }
            popup.add(removeItem)
        } else {
            // Add "Configure RPC" for predefined Bitcoin networks
            if (network.blockchainType == BlockchainType.BITCOIN) {
                val configureItem = JMenuItem("Configure RPC")
                configureItem.addActionListener {
                    val configDialog = BitcoinRpcConfigDialog(project, networkManager, network)
                    if (configDialog.showAndGet()) {
                        // Invalidate cached client so it reconnects with new credentials
                        BlockchainService.getInstance(project).invalidateNetwork(network.id)
                        refreshNetworkList()
                    }
                }
                popup.add(configureItem)
            }

            val disableItem = JMenuItem("Disable")
            disableItem.isEnabled = networkManager.selectedNetwork.value.id != network.id
            disableItem.addActionListener {
                networkManager.disableNetwork(network.id)
                refreshNetworkList()
            }
            popup.add(disableItem)
        }

        popup.show(component, 0, component.height)
    }

    private fun createAddCustomNetworkButton(): JComponent {
        val button = JButton("+ Add a custom network")
        button.alignmentX = Component.LEFT_ALIGNMENT
        button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        button.addActionListener {
            val dialog = AddCustomNetworkDialog(project, networkManager)
            if (dialog.showAndGet()) {
                refreshNetworkList()
            }
        }
        return button
    }

    private fun getNetworkInitial(network: Network): String {
        return when {
            network.symbol.equals("ETH", ignoreCase = true) -> "E"
            network.symbol.equals("BNB", ignoreCase = true) ||
                network.symbol.equals("tBNB", ignoreCase = true) -> "B"
            network.symbol.equals("MATIC", ignoreCase = true) -> "P"
            network.symbol.equals("TRX", ignoreCase = true) -> "T"
            else -> network.symbol.firstOrNull()?.uppercase() ?: "?"
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(cancelAction)
    }

    fun getSelectedNetwork(): Network? = selectedNetwork
}
