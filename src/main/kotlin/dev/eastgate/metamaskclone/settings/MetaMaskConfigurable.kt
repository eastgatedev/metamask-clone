package dev.eastgate.metamaskclone.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.network.PredefinedNetworks
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*
import javax.swing.border.TitledBorder

class MetaMaskConfigurable : Configurable {
    private var settingsComponent: MetaMaskSettingsComponent? = null

    override fun createComponent(): JComponent {
        settingsComponent = MetaMaskSettingsComponent()
        return settingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = MetaMaskSettings.instance
        val component = settingsComponent ?: return false

        return component.defaultNetwork != settings.defaultNetwork ||
            component.showTestnetWarning != settings.showTestnetWarning ||
            component.autoLockTimeout != settings.autoLockTimeout ||
            component.enableNotifications != settings.enableNotifications ||
            component.confirmTransactions != settings.confirmTransactions ||
            component.infuraApiKey != settings.infuraApiKey ||
            component.etherscanApiKey != settings.etherscanApiKey ||
            component.bscscanApiKey != settings.bscscanApiKey ||
            component.showBalanceInUSD != settings.showBalanceInUSD
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val settings = MetaMaskSettings.instance
        val component = settingsComponent ?: return

        settings.defaultNetwork = component.defaultNetwork
        settings.showTestnetWarning = component.showTestnetWarning
        settings.autoLockTimeout = component.autoLockTimeout
        settings.enableNotifications = component.enableNotifications
        settings.confirmTransactions = component.confirmTransactions
        settings.infuraApiKey = component.infuraApiKey
        settings.etherscanApiKey = component.etherscanApiKey
        settings.bscscanApiKey = component.bscscanApiKey
        settings.showBalanceInUSD = component.showBalanceInUSD
    }

    override fun reset() {
        val settings = MetaMaskSettings.instance
        val component = settingsComponent ?: return

        component.defaultNetwork = settings.defaultNetwork
        component.showTestnetWarning = settings.showTestnetWarning
        component.autoLockTimeout = settings.autoLockTimeout
        component.enableNotifications = settings.enableNotifications
        component.confirmTransactions = settings.confirmTransactions
        component.infuraApiKey = settings.infuraApiKey
        component.etherscanApiKey = settings.etherscanApiKey
        component.bscscanApiKey = settings.bscscanApiKey
        component.showBalanceInUSD = settings.showBalanceInUSD
    }

    override fun getDisplayName(): String {
        return "MetaMask Clone"
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}

class MetaMaskSettingsComponent {
    val panel: JPanel

    private val defaultNetworkCombo = JComboBox<String>()
    private val showTestnetWarningCheck = JBCheckBox("Show warning when using mainnet")
    private val autoLockTimeoutField = JBTextField()
    private val enableNotificationsCheck = JBCheckBox("Enable notifications")
    private val confirmTransactionsCheck = JBCheckBox("Confirm before sending transactions")
    private val infuraApiKeyField = JBTextField()
    private val etherscanApiKeyField = JBTextField()
    private val bscscanApiKeyField = JBTextField()
    private val showBalanceInUSDCheck = JBCheckBox("Show balance in USD")

    init {
        panel = JPanel(GridBagLayout())
        setupUI()
    }

    private fun setupUI() {
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.WEST
        gbc.insets = Insets(5, 5, 5, 5)

        // General Settings
        val generalPanel = createGeneralSettingsPanel()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.gridwidth = 2
        panel.add(generalPanel, gbc)

        // Security Settings
        val securityPanel = createSecuritySettingsPanel()
        gbc.gridy = 1
        panel.add(securityPanel, gbc)

        // API Keys
        val apiPanel = createApiKeysPanel()
        gbc.gridy = 2
        panel.add(apiPanel, gbc)

        // Add vertical glue to push everything to the top
        gbc.gridy = 3
        gbc.weighty = 1.0
        panel.add(Box.createVerticalGlue(), gbc)
    }

    private fun createGeneralSettingsPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("General Settings")

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(3, 5, 3, 5)

        // Default Network
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Default Network:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        PredefinedNetworks.ALL_NETWORKS.forEach { network ->
            defaultNetworkCombo.addItem(network.id)
        }
        panel.add(defaultNetworkCombo, gbc)

        // Show Balance in USD
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        panel.add(showBalanceInUSDCheck, gbc)

        // Enable Notifications
        gbc.gridy = 2
        panel.add(enableNotificationsCheck, gbc)

        return panel
    }

    private fun createSecuritySettingsPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("Security Settings")

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(3, 5, 3, 5)

        // Auto-lock timeout
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Auto-lock timeout (minutes):"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(autoLockTimeoutField, gbc)

        // Show testnet warning
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.gridwidth = 2
        panel.add(showTestnetWarningCheck, gbc)

        // Confirm transactions
        gbc.gridy = 2
        panel.add(confirmTransactionsCheck, gbc)

        return panel
    }

    private fun createApiKeysPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = TitledBorder("API Keys (Optional)")

        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(3, 5, 3, 5)

        // Infura API Key
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Infura API Key:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(infuraApiKeyField, gbc)

        // Etherscan API Key
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("Etherscan API Key:"), gbc)

        gbc.gridx = 1
        panel.add(etherscanApiKeyField, gbc)

        // BSCScan API Key
        gbc.gridx = 0
        gbc.gridy = 2
        panel.add(JLabel("BSCScan API Key:"), gbc)

        gbc.gridx = 1
        panel.add(bscscanApiKeyField, gbc)

        return panel
    }

    var defaultNetwork: String
        get() = defaultNetworkCombo.selectedItem as? String ?: "BNB_TESTNET"
        set(value) {
            defaultNetworkCombo.selectedItem = value
        }

    var showTestnetWarning: Boolean
        get() = showTestnetWarningCheck.isSelected
        set(value) {
            showTestnetWarningCheck.isSelected = value
        }

    var autoLockTimeout: Int
        get() = autoLockTimeoutField.text.toIntOrNull() ?: 10
        set(value) {
            autoLockTimeoutField.text = value.toString()
        }

    var enableNotifications: Boolean
        get() = enableNotificationsCheck.isSelected
        set(value) {
            enableNotificationsCheck.isSelected = value
        }

    var confirmTransactions: Boolean
        get() = confirmTransactionsCheck.isSelected
        set(value) {
            confirmTransactionsCheck.isSelected = value
        }

    var infuraApiKey: String
        get() = infuraApiKeyField.text
        set(value) {
            infuraApiKeyField.text = value
        }

    var etherscanApiKey: String
        get() = etherscanApiKeyField.text
        set(value) {
            etherscanApiKeyField.text = value
        }

    var bscscanApiKey: String
        get() = bscscanApiKeyField.text
        set(value) {
            bscscanApiKeyField.text = value
        }

    var showBalanceInUSD: Boolean
        get() = showBalanceInUSDCheck.isSelected
        set(value) {
            showBalanceInUSDCheck.isSelected = value
        }
}
