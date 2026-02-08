package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.blockchain.BitcoinRpcClient
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.Network
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.net.URLEncoder
import javax.swing.*

class BitcoinRpcConfigDialog(
    private val project: Project,
    private val networkManager: NetworkManager,
    private val network: Network
) : DialogWrapper(project) {
    private val hostField = JBTextField("127.0.0.1")
    private val portField = JBTextField(getDefaultPort())
    private val usernameField = JBTextField()
    private val passwordField = JPasswordField()
    private val testButton = JButton("Test Connection")
    private val statusLabel = JBLabel("")

    init {
        title = "Configure Bitcoin RPC - ${network.name}"
        prefillFromExisting()
        init()
    }

    private fun getDefaultPort(): String {
        return when (network.id) {
            "BTC_MAINNET" -> "8332"
            "BTC_TESTNET" -> "18332"
            "BTC_REGTEST" -> "18443"
            else -> "8332"
        }
    }

    private fun prefillFromExisting() {
        val existingUrl = networkManager.getEffectiveRpcUrl(network)
        try {
            val url = java.net.URL(existingUrl)
            if (url.host.isNotEmpty()) hostField.text = url.host
            if (url.port > 0) portField.text = url.port.toString()
            val userInfo = url.userInfo
            if (userInfo != null && userInfo != "user:pass") {
                val parts = userInfo.split(":", limit = 2)
                usernameField.text = java.net.URLDecoder.decode(parts[0], "UTF-8")
                if (parts.size > 1) {
                    passwordField.text = java.net.URLDecoder.decode(parts[1], "UTF-8")
                }
            }
        } catch (_: Exception) {
            // Use defaults
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // Host
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Host:"), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        hostField.columns = 25
        hostField.toolTipText = "Bitcoin Core RPC host (e.g., 127.0.0.1)"
        panel.add(hostField, gbc)

        // Port
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Port:"), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        portField.toolTipText = "RPC port (mainnet: 8332, testnet: 18332, regtest: 18443)"
        panel.add(portField, gbc)

        // Username
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Username:"), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        usernameField.toolTipText = "RPC username from bitcoin.conf (rpcuser)"
        panel.add(usernameField, gbc)

        // Password
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Password:"), gbc)
        gbc.gridx = 1
        gbc.weightx = 1.0
        passwordField.toolTipText = "RPC password from bitcoin.conf (rpcpassword)"
        panel.add(passwordField, gbc)

        // Test Connection button + status
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        gbc.gridwidth = 1
        testButton.addActionListener { testConnection() }
        panel.add(testButton, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(statusLabel, gbc)

        // Note
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        val noteLabel = JLabel(
            "<html><small>Configure your Bitcoin Core RPC credentials.<br>" +
                "These are set in your bitcoin.conf file (rpcuser/rpcpassword).</small></html>"
        )
        panel.add(noteLabel, gbc)

        return panel
    }

    private fun buildRpcUrl(): String? {
        val host = hostField.text.trim()
        val port = portField.text.trim()
        val username = usernameField.text.trim()
        val password = String(passwordField.password).trim()

        if (host.isEmpty() || port.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return null
        }

        val encodedUser = URLEncoder.encode(username, "UTF-8")
        val encodedPass = URLEncoder.encode(password, "UTF-8")

        return "http://$encodedUser:$encodedPass@$host:$port"
    }

    private fun testConnection() {
        val rpcUrl = buildRpcUrl()
        if (rpcUrl == null) {
            statusLabel.text = "<html><font color='red'>All fields are required</font></html>"
            return
        }

        testButton.isEnabled = false
        statusLabel.text = "Connecting..."

        Thread {
            try {
                val client = BitcoinRpcClient(rpcUrl)
                val info = client.getBlockchainInfo()
                val chain = info["chain"]?.asText() ?: "unknown"
                val blocks = info["blocks"]?.asInt() ?: 0
                client.close()

                SwingUtilities.invokeLater {
                    statusLabel.text = "<html><font color='green'>Connected: $chain, $blocks blocks</font></html>"
                    testButton.isEnabled = true
                }
            } catch (e: Exception) {
                val msg = e.message?.take(60) ?: "Connection failed"
                SwingUtilities.invokeLater {
                    statusLabel.text = "<html><font color='red'>$msg</font></html>"
                    testButton.isEnabled = true
                }
            }
        }.start()
    }

    override fun doOKAction() {
        val host = hostField.text.trim()
        val port = portField.text.trim()
        val username = usernameField.text.trim()
        val password = String(passwordField.password).trim()

        if (host.isEmpty()) {
            Messages.showErrorDialog("Please enter a host", "Validation Error")
            return
        }
        if (port.isEmpty() || port.toIntOrNull() == null) {
            Messages.showErrorDialog("Please enter a valid port number", "Validation Error")
            return
        }
        if (username.isEmpty()) {
            Messages.showErrorDialog("Please enter a username", "Validation Error")
            return
        }
        if (password.isEmpty()) {
            Messages.showErrorDialog("Please enter a password", "Validation Error")
            return
        }

        val rpcUrl = buildRpcUrl()!!
        networkManager.setBitcoinRpcConfig(network.id, rpcUrl)

        super.doOKAction()
    }
}
