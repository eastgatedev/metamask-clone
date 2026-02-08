plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    id("org.jetbrains.intellij.platform") version "2.7.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    kotlin("plugin.serialization") version "2.2.10"
}

group = "dev.eastgate.metamaskclone"
version = "1.4"

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    version.set("1.4.1")
    ignoreFailures.set(true)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }

    // Blockchain & Crypto Libraries
    implementation("org.web3j:core:4.10.3")
    implementation("org.web3j:crypto:4.10.3")
    implementation("org.web3j:utils:4.10.3")
    implementation("org.web3j:abi:4.10.3")
    implementation("org.web3j:contracts:4.10.3")

    // Mnemonic & HD Wallet - using bitcoinj for BIP39/BIP32/BIP44 support
    implementation("org.bitcoinj:bitcoinj-core:0.16.2") {
        exclude(group = "org.bouncycastle")
    }

    // JSON Handling
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines are provided by IntelliJ Platform - no need to include explicitly

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // QR Code generation
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")

    // Encryption
    implementation("org.bouncycastle:bcprov-jdk18on:1.78")

    implementation(files("libs/tron-wallet-1.0.jar"))

    // gRPC dependencies for Tron (required by tron-wallet-1.0.jar)
    implementation("io.grpc:grpc-netty:1.60.0")
    implementation("io.grpc:grpc-protobuf:1.60.0")
    implementation("io.grpc:grpc-stub:1.60.0")
    implementation("com.google.protobuf:protobuf-java:3.25.5")

    // Typesafe config (required by tron-wallet-1.0.jar crypto classes)
    implementation("com.typesafe:config:1.3.2")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("junit:junit:4.13.2") // Required for IntelliJ Platform test framework
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2") // JUnit 4 compatibility
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
    }

    // Plugin verification configuration
    pluginVerification {
        ides {
            ide("IC", "2025.1")
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    // Configure test task to use JUnit Platform
    test {
        useJUnitPlatform()
    }

    // Reformat code before build
    build {
        dependsOn(ktlintFormat)
    }

    // Skip buildSearchableOptions to avoid IntelliJ Platform 2025.1 memory leak warnings
    // (The warning is from IntelliJ's internal EmmetCompositeConfigurable, not this plugin)
    buildSearchableOptions {
        enabled = false
    }

    // Also skip dependent task since we disabled buildSearchableOptions
    named("prepareJarSearchableOptions") {
        enabled = false
    }

    named("jarSearchableOptions") {
        enabled = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // Use all-compatibility mode for interface defaults to avoid internal API bridge methods
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
