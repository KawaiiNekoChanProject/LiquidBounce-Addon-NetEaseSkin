import groovy.json.JsonSlurper
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.exclude
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.regex.Pattern

fun Project.addResolvedDependencies(
    from: Configuration,
    vararg toConfigurations: String,
) {
    val resolvedDeps = from.incoming.resolutionResult.allDependencies
        .map { dep ->
            val requested = dep.requested.displayName
            dependencies.create(requested) {
                (this as? ModuleDependency)?.isTransitive = false
            }
        }

    toConfigurations.forEach { configName ->
        configurations.named(configName).configure {
            withDependencies {
                addAll(resolvedDeps)
            }
        }
    }
}

fun Configuration.excludeProvidedLibs() = apply {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlinx", module = "atomicfu")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-datetime")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-io-bytestring")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-cbor")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")

    exclude(group = "it.unimi.dsi", module = "fastutil")
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "com.google.code.gson", module = "gson")
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "commons-codec", module = "commons-codec")
    exclude(group = "commons-io", module = "commons-io")
    exclude(group = "org.apache.commons", module = "commons-compress")
    exclude(group = "org.apache.commons", module = "commons-lang3")
    exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    exclude(group = "org.apache.logging.log4j", module = "log4j-api")
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j-impl")
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "com.mojang", module = "authlib")
    exclude(group = "org.lwjgl", module = "lwjgl")
}
