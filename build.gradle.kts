plugins {
    id("java")
    id("idea")
    id("eclipse")
    kotlin("jvm") version "2.1.0"
    id("net.neoforged.moddev") version "2.0.28-beta"
}

val modId = "opencomputers"
val modVersion = "3.0.0"
val minecraftVersion = "1.21.4"
val neoVersion = "21.4.0-beta"

version = modVersion
group = "li.cil.oc"
base {
    archivesName.set(modId)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

neoForge {
    version = neoVersion
    
    // parchment mappings disabled (network unavailable - use official names)
    // parchment {
    //     mappingsVersion = "2024.11.17"
    //     minecraftVersion = "1.21.4"
    // }
    
    runs {
        register("client") {
            client()
            gameDirectory.set(file("run"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            gameDirectory.set(file("run"))
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

sourceSets.main.configure {
    resources.srcDir("src/generated/resources")
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://www.cursemaven.com")
    maven("https://libraries.minecraft.net")
    maven("https://repo.maven.apache.org/maven2")
    maven("https://maven.parchmentmc.org")
}

// Separate configuration to resolve Kotlin JARs for the dev run classpath.
// NeoForge moddev runs the mod from raw class dirs (not a JAR), so jarJar
// bundling never applies. We resolve the JARs and add them via additionalRuntimeClasspathFile.
val kotlinRuntime: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    // LuaJ for embedded Lua runtime - include in mod JAR
    jarJar("org.luaj:luaj-jse:[3.0.1,4.0)") {
        isTransitive = false
    }
    implementation("org.luaj:luaj-jse:3.0.1")
    runtimeOnly("org.luaj:luaj-jse:3.0.1")

    // Kotlin stdlib - bundled in the mod JAR for production
    jarJar("org.jetbrains.kotlin:kotlin-stdlib:[2.0.0,3.0)") {
        isTransitive = false
    }
    implementation(kotlin("stdlib"))

    // Kotlin coroutines
    jarJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:[1.8.0,2.0)") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Kotlin reflect
    jarJar("org.jetbrains.kotlin:kotlin-reflect:[2.0.0,3.0)") {
        isTransitive = false
    }
    implementation(kotlin("reflect"))

    // JSON handling (Minecraft already has Gson, no need to bundle)
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin JARs resolved for dev run injection
    kotlinRuntime(kotlin("stdlib"))
    kotlinRuntime("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    kotlinRuntime(kotlin("reflect"))
}

// Write a classpath file listing the Kotlin JARs, then tell every NeoForge run
// to include it. This is the supported way to add extra JARs to moddev runs.
val writeKotlinClasspath by tasks.registering {
    val outFile = layout.buildDirectory.file("kotlin-runtime-classpath.txt")
    outputs.file(outFile)
    inputs.files(kotlinRuntime)
    doLast {
        outFile.get().asFile.writeText(
            kotlinRuntime.resolvedConfiguration.resolvedArtifacts
                .joinToString("\n") { it.file.absolutePath }
        )
    }
}

neoForge.runs.configureEach {
    additionalRuntimeClasspathFile(writeKotlinClasspath.map {
        layout.buildDirectory.file("kotlin-runtime-classpath.txt").get().asFile
    })
}

tasks.withType<ProcessResources>().configureEach {
    val properties = mapOf(
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion,
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_name" to "OpenComputers Rewrite",
        "mod_license" to "MIT",
        "mod_authors" to "Original: Sangar, Vexatos, payonel; Rewrite: Community",
        "mod_description" to "Modular computers and robots programmable in Lua"
    )
    inputs.properties(properties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(properties)
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({ configurations["jarJar"].filter { it.name.endsWith(".jar") }.map { zipTree(it) } }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
