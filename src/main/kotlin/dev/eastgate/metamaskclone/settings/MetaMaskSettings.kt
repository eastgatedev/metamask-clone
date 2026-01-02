package dev.eastgate.metamaskclone.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "dev.eastgate.metamaskclone.settings.MetaMaskSettings",
    storages = [Storage("MetaMaskCloneSettings.xml")]
)
class MetaMaskSettings : PersistentStateComponent<MetaMaskSettings> {
    var defaultNetwork: String = "BNB_TESTNET"
    var showTestnetWarning: Boolean = true
    var autoLockTimeout: Int = 10 // minutes
    var enableNotifications: Boolean = true
    var confirmTransactions: Boolean = true
    var gasLimitMultiplier: Double = 1.2
    var maxGasPrice: Long = 100 // Gwei
    var infuraApiKey: String = ""
    var etherscanApiKey: String = ""
    var bscscanApiKey: String = ""
    var polygonscanApiKey: String = ""
    var priceUpdateInterval: Long = 60000 // milliseconds
    var showBalanceInUSD: Boolean = true
    var defaultSlippage: Double = 0.5 // percentage

    override fun getState(): MetaMaskSettings {
        return this
    }

    override fun loadState(state: MetaMaskSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: MetaMaskSettings
            get() = service()
    }
}
