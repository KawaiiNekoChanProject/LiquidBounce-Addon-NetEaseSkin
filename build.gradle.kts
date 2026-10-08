plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
}

base {
    archivesName = project.property("archives_base_name") as String
    // The Minecraft version the add-on is built for goes into its version, e.g. 1.0.0+26.3
    version = "${project.property("mod_version")}+${libs.versions.minecraft.get()}"
    group = project.property("maven_group") as String
}

repositories {
    mavenCentral()
    // Lets you test against a locally built client (`./gradlew publishToMavenLocal` in LiquidBounce).
    mavenLocal()
    maven {
        name = "CCBlueX Releases"
        url = uri("https://maven.ccbluex.net/releases")
    }
    maven {
        name = "CCBlueX Snapshots"
        url = uri("https://maven.ccbluex.net/snapshots")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

val jij = configurations.create("jij")
jij.excludeProvidedLibs()

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.kotlin)

    // The client itself; there is no separate API artifact.
    implementation(libs.liquidbounce)

    jij(libs.wplauncher.helper)
    jij(libs.ktor.client.java)
}

addResolvedDependencies(jij, "compileOnly", "include", "api")

fun Configuration.excludeProvidedLibs() = apply {
    // fabric-language-kotlin
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

    // Minecraft
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

tasks.processResources {
    val modVersion = providers.gradleProperty("mod_version").zip(libs.versions.minecraft) { version, minecraft ->
        "$version+$minecraft"
    }
    val minecraftVersion = libs.versions.minecraft
    val loaderVersion = libs.versions.fabric.loader
    val fabricKotlinVersion = libs.versions.fabric.kotlin

    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    inputs.property("fabric_kotlin_version", fabricKotlinVersion)

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to modVersion.get(),
                "minecraft_version" to minecraftVersion.get(),
                "loader_version" to loaderVersion.get(),
                "fabric_kotlin_version" to fabricKotlinVersion.get(),
            )
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = libs.versions.jdk.get().toInt()
}

java {
    withSourcesJar()

    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.jdk.get().toInt())
    }
}

kotlin {
    compilerOptions {
        jvmToolchain(libs.versions.jdk.get().toInt())
        // LiquidBounce is compiled with preview features, which marks its classes as pre-release
        freeCompilerArgs.add("-Xskip-prerelease-check")
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}
