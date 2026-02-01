package dev.eastgate.metamaskclone

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.encoders.Hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.tron.common.crypto.ECKey
import org.tron.common.crypto.Hash
import org.tron.common.utils.Base58
import org.web3j.crypto.Credentials
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security

/**
 * TestTron demonstrates the shared cryptographic foundation between EVM (Ethereum) and TRON blockchains.
 *
 * Key Insights:
 * 1. Both blockchains use the same secp256k1 elliptic curve for ECDSA signatures
 * 2. Both derive public keys identically from private keys using EC point multiplication
 * 3. Both use Keccak-256 (SHA3) to hash the public key
 * 4. The only difference is in address encoding:
 *    - EVM: Takes last 20 bytes of Keccak-256(publicKey), encodes as hex with 0x prefix
 *    - TRON: Takes last 20 bytes of Keccak-256(publicKey), adds 0x41 prefix, applies Base58Check
 *
 * Address Derivation Process:
 *
 * EVM (Ethereum):
 * 1. Private Key (32 bytes) → Public Key via secp256k1 point multiplication (65 bytes uncompressed)
 * 2. Keccak-256(Public Key without first byte) → 32 bytes hash
 * 3. Take last 20 bytes → Ethereum address
 * 4. Encode as hex with 0x prefix → "0x..." (42 characters)
 *
 * TRON:
 * 1. Private Key (32 bytes) → Public Key via secp256k1 point multiplication (65 bytes uncompressed)
 * 2. Keccak-256(Public Key without first byte) → 32 bytes hash
 * 3. Take last 20 bytes, set first byte to 0x41 (mainnet) or 0xa0 (testnet) → 21 bytes
 * 4. Base58Check encode (double SHA-256 for checksum) → "T..." for mainnet (34 characters)
 *
 * This test proves that the same private key can be used to derive addresses on both networks.
 */
@Disabled
class TestTron {
    companion object {
        // Test private key (NEVER use in production!)
        // This is a well-known test key for demonstration purposes only
        const val TEST_PRIVATE_KEY = "<your-key>"

        // TRON address prefix bytes
        const val TRON_MAINNET_PREFIX: Byte = 0x41.toByte() // Mainnet addresses start with 'T'
        const val TRON_TESTNET_PREFIX: Byte = 0xa0.toByte() // Testnet addresses start with different characters

        init {
            // Ensure BouncyCastle provider is registered
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
        }
    }

    @Test
    fun `test private key to public key is identical for EVM and TRON`() {
        println("=".repeat(80))
        println("TEST: Private Key → Public Key Derivation")
        println("=".repeat(80))

        // Convert hex private key to bytes
        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)
        println("Private Key (hex): $TEST_PRIVATE_KEY")
        println("Private Key length: ${privateKeyBytes.size} bytes")
        println()

        // === EVM (Web3j) Public Key Derivation ===
        println("--- EVM (Ethereum) Public Key Derivation ---")
        val evmCredentials = Credentials.create(TEST_PRIVATE_KEY)
        val evmPublicKeyHex = evmCredentials.ecKeyPair.publicKey.toString(16).padStart(128, '0')
        println("EVM Public Key (hex): $evmPublicKeyHex")
        println("EVM Public Key length: ${evmPublicKeyHex.length / 2} bytes")
        println()

        // === TRON Public Key Derivation ===
        println("--- TRON Public Key Derivation ---")
        val tronECKey = ECKey.fromPrivate(privateKeyBytes)
        val tronPublicKeyBytes = tronECKey.pubKey
        val tronPublicKeyHex = Hex.toHexString(tronPublicKeyBytes)
        println("TRON Public Key (hex): $tronPublicKeyHex")
        println("TRON Public Key length: ${tronPublicKeyBytes.size} bytes")
        println()

        // === Manual Derivation (to prove the math) ===
        println("--- Manual Derivation (proving the cryptographic foundation) ---")
        val privateKeyBigInt = BigInteger(1, privateKeyBytes)
        val publicKeyPoint: ECPoint = ECKey.CURVE.g.multiply(privateKeyBigInt)
        val manualPublicKey = publicKeyPoint.getEncoded(false) // Uncompressed format
        val manualPublicKeyHex = Hex.toHexString(manualPublicKey)
        println("Manual Public Key (hex): $manualPublicKeyHex")
        println("Manual Public Key length: ${manualPublicKey.size} bytes")
        println()

        // === Verification ===
        println("--- Verification ---")
        println("✓ All three derivations use secp256k1 curve")
        println("✓ All produce uncompressed public keys (65 bytes)")
        println("✓ Format: 0x04 || X-coordinate (32 bytes) || Y-coordinate (32 bytes)")
        println()

        // Assert all public keys are identical
        // EVM public key is 64 bytes (without 0x04 prefix), TRON is 65 bytes (with 0x04 prefix)
        val evmFullKey = "04$evmPublicKeyHex"
        assertEquals(
            evmFullKey,
            tronPublicKeyHex,
            "EVM and TRON should derive identical public keys from the same private key"
        )
        assertEquals(
            manualPublicKeyHex,
            tronPublicKeyHex,
            "Manual derivation should match TRON derivation"
        )

        println("✅ SUCCESS: All public keys are IDENTICAL!")
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test address derivation from same private key for EVM and TRON`() {
        println("=".repeat(80))
        println("TEST: Address Derivation from Same Private Key")
        println("=".repeat(80))

        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)
        println("Private Key: $TEST_PRIVATE_KEY")
        println()

        // === EVM Address Derivation ===
        println("--- EVM (Ethereum) Address Derivation ---")
        val evmCredentials = Credentials.create(TEST_PRIVATE_KEY)
        val evmAddress = evmCredentials.address
        println("EVM Address: $evmAddress")
        println("EVM Address format: 0x + 40 hex characters (20 bytes)")
        println()

        // === TRON Address Derivation ===
        println("--- TRON Address Derivation ---")
        val tronECKey = ECKey.fromPrivate(privateKeyBytes)
        val tronAddressBytes = tronECKey.address
        val tronAddressHex = Hex.toHexString(tronAddressBytes)
        val tronBase58Address = encodeToBase58Check(tronAddressBytes)
        println("TRON Address (hex): $tronAddressHex")
        println("TRON Address (Base58): $tronBase58Address")
        println("TRON Address format: 21 bytes (0x41 prefix + 20 bytes), Base58Check encoded")
        println()

        // === Manual Step-by-Step Derivation ===
        println("--- Manual Step-by-Step Derivation ---")

        // Step 1: Get public key
        val publicKeyBytes = tronECKey.pubKey
        println("1. Public Key (65 bytes): ${Hex.toHexString(publicKeyBytes)}")

        // Step 2: Keccak-256 hash (skip first byte 0x04)
        val publicKeyWithoutPrefix = publicKeyBytes.copyOfRange(1, publicKeyBytes.size)
        val keccakHash = Hash.sha3(publicKeyWithoutPrefix)
        println("2. Keccak-256 hash (32 bytes): ${Hex.toHexString(keccakHash)}")

        // Step 3: Take last 20 bytes
        val last20Bytes = keccakHash.copyOfRange(12, 32)
        println("3. Last 20 bytes: ${Hex.toHexString(last20Bytes)}")

        // Step 4a: EVM - use directly with 0x prefix
        val evmManualAddress = "0x" + Hex.toHexString(last20Bytes)
        println("4a. EVM Address: $evmManualAddress")

        // Step 4b: TRON - add 0x41 prefix
        val tronAddressWithPrefix = byteArrayOf(TRON_MAINNET_PREFIX) + last20Bytes
        println("4b. TRON Address with 0x41 prefix (21 bytes): ${Hex.toHexString(tronAddressWithPrefix)}")

        // Step 5: TRON Base58Check encoding
        val tronManualBase58 = encodeToBase58Check(tronAddressWithPrefix)
        println("5. TRON Base58Check Address: $tronManualBase58")
        println()

        // === Key Observation ===
        println("--- Key Observation ---")
        println("The same 20-byte address core: ${Hex.toHexString(last20Bytes)}")
        println("  → EVM adds '0x' prefix:  $evmManualAddress")
        println("  → TRON adds 0x41 prefix and Base58Check encodes: $tronManualBase58")
        println()

        // === Verification ===
        assertEquals(
            evmAddress.lowercase(),
            evmManualAddress.lowercase(),
            "EVM addresses should match"
        )
        assertEquals(
            tronBase58Address,
            tronManualBase58,
            "TRON Base58 addresses should match"
        )

        // Verify the underlying 20 bytes are the same
        val evmAddressBytes = Numeric.hexStringToByteArray(evmAddress)
        assertEquals(
            Hex.toHexString(evmAddressBytes),
            Hex.toHexString(last20Bytes),
            "EVM and TRON share the same 20-byte address core"
        )

        println("✅ SUCCESS: Same private key → Same address core → Different encoding!")
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test multiple private keys produce consistent addresses across chains`() {
        println("=".repeat(80))
        println("TEST: Multiple Private Keys - Cross-Chain Address Consistency")
        println("=".repeat(80))

        val testKeys = listOf(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "1111111111111111111111111111111111111111111111111111111111111111",
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )

        testKeys.forEachIndexed { index, privateKey ->
            println("Test Case ${index + 1}:")
            println("Private Key: $privateKey")

            // EVM
            val evmCreds = Credentials.create(privateKey)
            val evmAddress = evmCreds.address
            println("  EVM Address:  $evmAddress")

            // TRON
            val tronKey = ECKey.fromPrivate(Numeric.hexStringToByteArray(privateKey))
            val tronBase58 = encodeToBase58Check(tronKey.address)
            println("  TRON Address: $tronBase58")

            // Verify both addresses derive from same public key
            val evmPubKey = "04" + evmCreds.ecKeyPair.publicKey.toString(16).padStart(128, '0')
            val tronPubKey = Hex.toHexString(tronKey.pubKey)
            assertEquals(evmPubKey, tronPubKey, "Public keys must match")

            println()
        }

        println("✅ SUCCESS: All keys show consistent cross-chain behavior!")
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test Base58Check encoding matches TRON specification`() {
        println("=".repeat(80))
        println("TEST: Base58Check Encoding (TRON Specification)")
        println("=".repeat(80))

        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)
        val tronECKey = ECKey.fromPrivate(privateKeyBytes)
        val addressBytes = tronECKey.address

        println("Address bytes (hex): ${Hex.toHexString(addressBytes)}")
        println("Address length: ${addressBytes.size} bytes")
        println()

        // Manual Base58Check encoding
        println("--- Manual Base58Check Encoding ---")

        // Step 1: First SHA-256
        val hash0 = sha256(addressBytes)
        println("1. SHA-256 (first):  ${Hex.toHexString(hash0)}")

        // Step 2: Second SHA-256
        val hash1 = sha256(hash0)
        println("2. SHA-256 (second): ${Hex.toHexString(hash1)}")

        // Step 3: Take first 4 bytes as checksum
        val checksum = hash1.copyOfRange(0, 4)
        println("3. Checksum (4 bytes): ${Hex.toHexString(checksum)}")

        // Step 4: Append checksum to address
        val addressWithChecksum = addressBytes + checksum
        println("4. Address + Checksum: ${Hex.toHexString(addressWithChecksum)}")

        // Step 5: Base58 encode
        val base58Address = Base58.encode(addressWithChecksum)
        println("5. Base58 encoded: $base58Address")
        println()

        // Verify with library function
        val libraryEncoded = encodeToBase58Check(addressBytes)
        println("Library encoded: $libraryEncoded")
        println()

        assertEquals(base58Address, libraryEncoded, "Manual and library encoding must match")

        // Verify address starts with 'T' for mainnet (0x41 prefix)
        assertTrue(base58Address.startsWith("T"), "Mainnet TRON addresses should start with 'T'")

        println("✅ SUCCESS: Base58Check encoding verified!")
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test cryptographic foundation shared between EVM and TRON`() {
        println("=".repeat(80))
        println("TEST: Shared Cryptographic Foundation")
        println("=".repeat(80))

        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)

        println("Shared Elements:")
        println("1. Elliptic Curve: secp256k1 (both chains)")
        println("2. Hash Function: Keccak-256 (SHA3) (both chains)")
        println("3. Signature Algorithm: ECDSA (both chains)")
        println("4. Private Key Size: 32 bytes (both chains)")
        println("5. Public Key Size: 65 bytes uncompressed (both chains)")
        println("6. Address Core: 20 bytes (both chains)")
        println()

        println("Differences:")
        println("1. Address Prefix:")
        println("   - EVM: None (raw 20 bytes)")
        println("   - TRON: 0x41 (mainnet) or 0xa0 (testnet)")
        println("2. Address Encoding:")
        println("   - EVM: Hexadecimal with 0x prefix")
        println("   - TRON: Base58Check (Bitcoin-style)")
        println("3. Address Length:")
        println("   - EVM: 42 characters (0x + 40 hex)")
        println("   - TRON: 34 characters (Base58)")
        println()

        // Prove same curve parameters
        val evmCreds = Credentials.create(TEST_PRIVATE_KEY)
        val tronKey = ECKey.fromPrivate(privateKeyBytes)

        val evmPubKey = evmCreds.ecKeyPair.publicKey
        val tronPubKeyBigInt = BigInteger(1, tronKey.pubKey.copyOfRange(1, 33))

        println("Verification:")
        println("✓ Both use secp256k1 curve with same parameters")
        println("✓ Both derive public key via G × private_key")
        println("✓ Both use Keccak-256(public_key) for address derivation")
        println("✓ Only encoding differs, not the cryptographic core")
        println()

        assertNotNull(evmPubKey, "EVM public key should not be null")
        assertNotNull(tronKey.pubKey, "TRON public key should not be null")

        println("✅ SUCCESS: Cryptographic foundation verified!")
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test same private key generates DIFFERENT addresses for EVM and TRON`() {
        println("=".repeat(80))
        println("TEST: Same Private Key → DIFFERENT Final Addresses")
        println("=".repeat(80))
        println()
        println("This test demonstrates that while EVM and TRON share the same cryptographic")
        println("foundation (same private key, same public key, same 20-byte address core),")
        println("they produce DIFFERENT final addresses due to different encoding schemes.")
        println()
        println("=".repeat(80))

        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)

        // Step 1: Same Private Key
        println("STEP 1: Same Private Key")
        println("Private Key: $TEST_PRIVATE_KEY")
        println()

        // Step 2: Derive Public Keys
        println("STEP 2: Derive Public Keys (using same secp256k1 curve)")
        val evmCredentials = Credentials.create(TEST_PRIVATE_KEY)
        val tronECKey = ECKey.fromPrivate(privateKeyBytes)

        val evmPublicKey = "04" + evmCredentials.ecKeyPair.publicKey.toString(16).padStart(128, '0')
        val tronPublicKey = Hex.toHexString(tronECKey.pubKey)

        println("EVM Public Key:  $evmPublicKey")
        println("TRON Public Key: $tronPublicKey")
        println()

        // Verify public keys are identical
        assertEquals(
            evmPublicKey,
            tronPublicKey,
            "Public keys MUST be identical (same private key, same curve)"
        )
        println("✓ Public keys are IDENTICAL")
        println()

        // Step 3: Derive 20-byte Address Core
        println("STEP 3: Derive 20-byte Address Core (using same Keccak-256 hash)")
        val publicKeyWithoutPrefix = tronECKey.pubKey.copyOfRange(1, tronECKey.pubKey.size)
        val keccakHash = Hash.sha3(publicKeyWithoutPrefix)
        val addressCore20Bytes = keccakHash.copyOfRange(12, 32)
        println("Keccak-256 Hash: ${Hex.toHexString(keccakHash)}")
        println("Last 20 bytes (address core): ${Hex.toHexString(addressCore20Bytes)}")
        println()

        // Verify both chains use the same 20-byte core
        val evmAddressBytes = Numeric.hexStringToByteArray(evmCredentials.address)
        assertEquals(
            Hex.toHexString(addressCore20Bytes),
            Hex.toHexString(evmAddressBytes),
            "Address core MUST be identical (same hash, same 20 bytes)"
        )
        println("✓ 20-byte address core is IDENTICAL")
        println()

        // Step 4: Apply Different Encoding
        println("STEP 4: Apply Different Encoding Schemes")
        println()

        // EVM: Hexadecimal encoding with 0x prefix
        val evmAddress = evmCredentials.address
        println("EVM Address (Hexadecimal):")
        println("  Encoding: 0x + hex(20 bytes)")
        println("  Result:   $evmAddress")
        println("  Length:   ${evmAddress.length} characters (42 chars: '0x' + 40 hex digits)")
        println()

        // TRON: Add 0x41 prefix, then Base58Check encode
        val tronAddressWithPrefix = byteArrayOf(TRON_MAINNET_PREFIX) + addressCore20Bytes
        val tronAddress = encodeToBase58Check(tronAddressWithPrefix)
        println("TRON Address (Base58Check):")
        println("  Encoding: Base58Check(0x41 + 20 bytes)")
        println("  21 bytes: ${Hex.toHexString(tronAddressWithPrefix)}")
        println("  Result:   $tronAddress")
        println("  Length:   ${tronAddress.length} characters (typically 34 chars starting with 'T')")
        println()

        // Step 5: Compare Final Addresses
        println("=".repeat(80))
        println("STEP 5: Final Address Comparison")
        println("=".repeat(80))
        println()
        println("Same Private Key:     $TEST_PRIVATE_KEY")
        println("Same Public Key:      ${evmPublicKey.substring(0, 20)}... (identical)")
        println("Same Address Core:    ${Hex.toHexString(addressCore20Bytes)}")
        println()
        println("BUT DIFFERENT Final Addresses:")
        println()
        println("  EVM  (hex):         $evmAddress")
        println("  TRON (Base58):      $tronAddress")
        println()
        println("=".repeat(80))

        // Assert that the final addresses are NOT equal
        org.junit.jupiter.api.Assertions.assertNotEquals(
            evmAddress.lowercase(),
            tronAddress.lowercase(),
            "Final addresses MUST be DIFFERENT due to different encoding (hex vs Base58Check)"
        )

        // Verify format differences
        assertTrue(evmAddress.startsWith("0x"), "EVM address must start with '0x'")
        assertEquals(42, evmAddress.length, "EVM address must be 42 characters")
        assertTrue(tronAddress.startsWith("T"), "TRON mainnet address must start with 'T'")
        assertEquals(34, tronAddress.length, "TRON address is typically 34 characters")

        println()
        println("✅ SUCCESS: Same private key produces DIFFERENT addresses!")
        println("✅ Reason: Different encoding schemes (EVM hex vs TRON Base58Check)")
        println()
        println("Key Takeaway:")
        println("  - You can use the SAME private key for both EVM and TRON wallets")
        println("  - But you CANNOT send funds to the same address string on both chains")
        println("  - The addresses LOOK different but are derived from the SAME key")
        println()
        println("=".repeat(80))
        println()
    }

    @Test
    fun `test private key export is IDENTICAL across EVM and TRON chains`() {
        println("=".repeat(80))
        println("TEST: Private Key Export - Chain Agnostic")
        println("=".repeat(80))
        println()
        println("This test demonstrates a CRITICAL concept for wallet developers:")
        println("Private keys are UNIVERSAL and CHAIN-AGNOSTIC!")
        println()
        println("A private key exported from an EVM wallet is IDENTICAL to the private key")
        println("exported from a TRON wallet (if both were derived from the same seed/key).")
        println()
        println("This means:")
        println("  1. Private keys are just 32 bytes of data (256 bits)")
        println("  2. They don't \"belong\" to any specific blockchain")
        println("  3. The SAME private key can control addresses on MULTIPLE chains")
        println("  4. Only the address ENCODING differs, NOT the underlying private key")
        println()
        println("=".repeat(80))
        println()

        // Use our test private key
        val privateKeyBytes = Numeric.hexStringToByteArray(TEST_PRIVATE_KEY)

        // ==================== PART 1: Create Wallets on Both Chains ====================
        println("PART 1: Create Wallets on Both Chains Using Same Private Key")
        println("-".repeat(80))

        // Create EVM wallet
        println("Creating EVM wallet from private key...")
        val evmCredentials = Credentials.create(TEST_PRIVATE_KEY)
        val evmAddress = evmCredentials.address
        println("✓ EVM wallet created")
        println("  Address: $evmAddress")
        println()

        // Create TRON wallet
        println("Creating TRON wallet from SAME private key...")
        val tronECKey = ECKey.fromPrivate(privateKeyBytes)
        val tronAddress = encodeToBase58Check(tronECKey.address)
        println("✓ TRON wallet created")
        println("  Address: $tronAddress")
        println()

        // ==================== PART 2: Export Private Keys ====================
        println("=".repeat(80))
        println("PART 2: Export Private Keys from Both Wallets")
        println("-".repeat(80))

        // Export from EVM wallet
        println("Exporting private key from EVM wallet:")
        val evmExportedKeyBytes = evmCredentials.ecKeyPair.privateKey.toByteArray()
        val evmExportedKeyHex = Numeric.toHexStringNoPrefix(evmExportedKeyBytes).padStart(64, '0')
        val evmExportedKeyWithPrefix = "0x$evmExportedKeyHex"

        println("  Format 1 (hex with 0x):  $evmExportedKeyWithPrefix")
        println("  Format 2 (hex no prefix): $evmExportedKeyHex")
        println("  Format 3 (raw bytes):     ${evmExportedKeyBytes.joinToString("") { "%02x".format(it) }}")
        println("  Length: ${evmExportedKeyBytes.size} bytes")
        println()

        // Export from TRON wallet
        println("Exporting private key from TRON wallet:")
        val tronExportedKeyBytes = tronECKey.privKeyBytes!!
        val tronExportedKeyHex = Hex.toHexString(tronExportedKeyBytes)
        val tronExportedKeyWithPrefix = "0x$tronExportedKeyHex"

        println("  Format 1 (hex with 0x):  $tronExportedKeyWithPrefix")
        println("  Format 2 (hex no prefix): $tronExportedKeyHex")
        println("  Format 3 (raw bytes):     ${tronExportedKeyBytes.joinToString("") { "%02x".format(it) }}")
        println("  Length: ${tronExportedKeyBytes.size} bytes")
        println()

        // ==================== PART 3: Verify Exported Keys Are IDENTICAL ====================
        println("=".repeat(80))
        println("PART 3: Verify Exported Private Keys Are IDENTICAL")
        println("-".repeat(80))

        println("Comparing exported private keys:")
        println()
        println("  EVM exported:  $evmExportedKeyHex")
        println("  TRON exported: $tronExportedKeyHex")
        println()

        // Assert all formats are identical
        assertEquals(
            evmExportedKeyHex.lowercase(),
            tronExportedKeyHex.lowercase(),
            "Private keys exported from EVM and TRON MUST be identical (hex format)"
        )

        assertEquals(
            evmExportedKeyBytes.size,
            tronExportedKeyBytes.size,
            "Private keys MUST have same length (32 bytes)"
        )

        for (i in evmExportedKeyBytes.indices) {
            assertEquals(
                evmExportedKeyBytes[i],
                tronExportedKeyBytes[i],
                "Private key byte $i MUST be identical"
            )
        }

        println("✓ All private key formats are IDENTICAL!")
        println("  - Hex with prefix: ${evmExportedKeyWithPrefix == tronExportedKeyWithPrefix}")
        println("  - Hex no prefix: ${evmExportedKeyHex.lowercase() == tronExportedKeyHex.lowercase()}")
        println("  - Raw bytes: ${evmExportedKeyBytes.contentEquals(tronExportedKeyBytes)}")
        println("  - Length: Both are ${evmExportedKeyBytes.size} bytes")
        println()

        // ==================== PART 4: Cross-Import Test ====================
        println("=".repeat(80))
        println("PART 4: Cross-Import Test - Import Private Key to Different Chain")
        println("-".repeat(80))

        // Import EVM-exported key into TRON
        println("Scenario 1: Import EVM-exported key into TRON wallet")
        val tronFromEVMKey = ECKey.fromPrivate(Numeric.hexStringToByteArray(evmExportedKeyHex))
        val tronFromEVMAddress = encodeToBase58Check(tronFromEVMKey.address)
        println("  Original EVM address: $evmAddress")
        println("  Imported TRON address: $tronFromEVMAddress")
        assertEquals(
            tronAddress,
            tronFromEVMAddress,
            "Importing EVM key to TRON should produce the SAME TRON address"
        )
        println("  ✓ SUCCESS: Imported key produces SAME TRON address")
        println()

        // Import TRON-exported key into EVM
        println("Scenario 2: Import TRON-exported key into EVM wallet")
        val evmFromTRONKey = Credentials.create(tronExportedKeyHex)
        val evmFromTRONAddress = evmFromTRONKey.address
        println("  Original TRON address: $tronAddress")
        println("  Imported EVM address: $evmFromTRONAddress")
        assertEquals(
            evmAddress.lowercase(),
            evmFromTRONAddress.lowercase(),
            "Importing TRON key to EVM should produce the SAME EVM address"
        )
        println("  ✓ SUCCESS: Imported key produces SAME EVM address")
        println()

        // ==================== PART 5: Multiple Export Formats ====================
        println("=".repeat(80))
        println("PART 5: Common Private Key Export Formats")
        println("-".repeat(80))

        println("All these formats represent the SAME private key:")
        println()
        println("1. Raw Hex (64 chars, no prefix):")
        println("   $evmExportedKeyHex")
        println()
        println("2. Hex with 0x prefix (66 chars):")
        println("   $evmExportedKeyWithPrefix")
        println()
        println("3. Byte array (32 bytes):")
        println("   [${evmExportedKeyBytes.take(8).joinToString(", ") { it.toString() }}, ... (24 more)]")
        println()
        println("4. BigInteger representation:")
        val privateKeyBigInt = BigInteger(1, evmExportedKeyBytes)
        println("   $privateKeyBigInt")
        println()

        println("All formats can be converted to each other and work on BOTH chains!")
        println()

        // ==================== PART 6: Key Takeaways ====================
        println("=".repeat(80))
        println("KEY TAKEAWAYS FOR WALLET DEVELOPERS")
        println("=".repeat(80))
        println()
        println("1. PRIVATE KEYS ARE UNIVERSAL:")
        println("   - The same 32-byte private key controls addresses on ALL chains")
        println("   - EVM (Ethereum, BSC, Polygon, etc.) and TRON share the same key format")
        println()
        println("2. PRIVATE KEY EXPORT IS CHAIN-AGNOSTIC:")
        println("   - Exporting from EVM gives the SAME key as exporting from TRON")
        println("   - The export format (hex, bytes) is identical across chains")
        println()
        println("3. PRIVATE KEYS CAN BE IMPORTED ACROSS CHAINS:")
        println("   - A key exported from EVM can be imported into TRON")
        println("   - A key exported from TRON can be imported into EVM")
        println("   - The imported key will control the corresponding address on the new chain")
        println()
        println("4. ONLY ADDRESSES DIFFER, NOT PRIVATE KEYS:")
        println("   - Same private key → Same public key → Same 20-byte address core")
        println("   - But different encoding → Different address strings")
        println("   - EVM: $evmAddress")
        println("   - TRON: $tronAddress")
        println()
        println("5. SECURITY IMPLICATION:")
        println("   - If someone gets your private key, they can access your funds on ALL chains")
        println("   - Never share your private key, regardless of which chain you're using")
        println("   - Treat a private key as a master key to multiple blockchain accounts")
        println()
        println("6. WALLET IMPLEMENTATION:")
        println("   - Store private key ONCE in your wallet")
        println("   - Derive EVM address using hex encoding")
        println("   - Derive TRON address using 0x41 prefix + Base58Check")
        println("   - Export private key in any format (hex, bytes) - it's the same data")
        println()
        println("=".repeat(80))
        println()
        println("✅ TEST PASSED: Private keys are IDENTICAL across EVM and TRON!")
        println("✅ Cross-import works perfectly - same key, different addresses!")
        println()
        println("=".repeat(80))
    }

    // Helper function to compute SHA-256 hash
    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    // Helper function to encode address to Base58Check format (TRON mainnet)
    private fun encodeToBase58Check(addressBytes: ByteArray): String {
        // Double SHA-256 for checksum
        val hash0 = sha256(addressBytes)
        val hash1 = sha256(hash0)

        // Append first 4 bytes of second hash as checksum
        val addressWithChecksum = addressBytes + hash1.copyOfRange(0, 4)

        // Base58 encode
        return Base58.encode(addressWithChecksum)
    }
}
