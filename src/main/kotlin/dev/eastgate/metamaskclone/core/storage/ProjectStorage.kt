package dev.eastgate.metamaskclone.core.storage

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Token
import dev.eastgate.metamaskclone.models.Wallet
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
@State(
    name = "data",
    storages = [Storage("metamask-clone.xml")]
)
class ProjectStorage : PersistentStateComponent<ProjectStorage.State> {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    data class State(
        var wallets: String = "[]",
        var selectedNetwork: String = "BNB_TESTNET",
        var customNetworks: String = "[]",
        var enabledNetworkIds: String = "[\"BNB_TESTNET\", \"ETH_SEPOLIA\", \"POLYGON_TESTNET\"]",
        var tokens: String = "[]",
        var encryptedMasterPassword: String? = null,
        var settings: String = "{}",
        var bitcoinRpcConfigs: String = "{}"
    )

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun getWallets(): List<Wallet> {
        return try {
            json.decodeFromString(myState.wallets)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWallets(wallets: List<Wallet>) {
        myState.wallets = json.encodeToString(wallets)
    }

    fun getSelectedNetwork(): String {
        return myState.selectedNetwork
    }

    fun setSelectedNetwork(network: String) {
        myState.selectedNetwork = network
    }

    fun getCustomNetworks(): List<Network> {
        return try {
            json.decodeFromString(myState.customNetworks)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCustomNetworks(networks: List<Network>) {
        myState.customNetworks = json.encodeToString(networks)
    }

    fun getEnabledNetworkIds(): List<String> {
        return try {
            json.decodeFromString(myState.enabledNetworkIds)
        } catch (e: Exception) {
            listOf("BNB_TESTNET", "ETH_SEPOLIA", "POLYGON_TESTNET")
        }
    }

    fun setEnabledNetworkIds(ids: List<String>) {
        myState.enabledNetworkIds = json.encodeToString(ids)
    }

    fun getTokens(): List<Token> {
        return try {
            json.decodeFromString(myState.tokens)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveTokens(tokens: List<Token>) {
        myState.tokens = json.encodeToString(tokens)
    }

    fun getMasterPassword(): String? {
        return myState.encryptedMasterPassword
    }

    fun setMasterPassword(encryptedPassword: String) {
        myState.encryptedMasterPassword = encryptedPassword
    }

    fun getSettings(): Map<String, String> {
        return try {
            json.decodeFromString(myState.settings)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveSettings(settings: Map<String, String>) {
        myState.settings = json.encodeToString(settings)
    }

    fun getBitcoinRpcConfigs(): Map<String, String> {
        return try {
            json.decodeFromString(myState.bitcoinRpcConfigs)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveBitcoinRpcConfigs(configs: Map<String, String>) {
        myState.bitcoinRpcConfigs = json.encodeToString(configs)
    }

    fun getBitcoinRpcUrl(networkId: String): String? {
        return getBitcoinRpcConfigs()[networkId]
    }

    fun setBitcoinRpcUrl(
        networkId: String,
        rpcUrl: String
    ) {
        val configs = getBitcoinRpcConfigs().toMutableMap()
        configs[networkId] = rpcUrl
        saveBitcoinRpcConfigs(configs)
    }

    fun clearAllData() {
        myState = State()
    }

    companion object {
        fun getInstance(project: Project): ProjectStorage {
            return project.getService(ProjectStorage::class.java)
        }
    }
}

// Network model
@kotlinx.serialization.Serializable
data class Network(
    val id: String,
    val name: String,
    val rpcUrl: String,
    val chainId: Int,
    val symbol: String,
    val blockExplorerUrl: String? = null,
    val isTestnet: Boolean = false,
    val isCustom: Boolean = false,
    val blockchainType: BlockchainType = BlockchainType.EVM
)
