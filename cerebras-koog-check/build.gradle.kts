import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation("ai.koog:koog-agents:0.7.1")
    implementation("ai.koog:agents-core:0.7.1")
    implementation("ai.koog:prompt-executor-model:0.7.1")
    implementation("ai.koog:prompt-executor-clients:0.7.1")
    implementation("ai.koog:prompt-llm:0.7.1")
}

application {
    mainClass = "com.dailycurator.tools.CerebrasKoogCheckKt"
}
