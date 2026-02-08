package dev.eastgate.metamaskclone.models

import kotlinx.serialization.Serializable

/**
 * Represents the type of blockchain network.
 * Used to differentiate between EVM-compatible chains, TRON, and Bitcoin.
 */
@Serializable
enum class BlockchainType {
    EVM,
    TRON,
    BITCOIN
}
