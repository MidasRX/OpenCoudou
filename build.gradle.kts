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

dependencies {
    // LuaJ for embedded Lua runtime - include in mod JAR
    jarJar("org.luaj:luaj-jse:[3.0.1,4.0)") {
        isTransitive = false
    }
    implementation("org.luaj:luaj-jse:3.0.1")
    runtimeOnly("org.luaj:luaj-jse:3.0.1")

    // Kotlin stdlib - bundled in the mod JAR for production.
    // Also added to additionalRuntimeClasspath so NeoForge dev runs can find it
    // (dev runs load from raw class dirs, not the JAR, so jarJar doesn't apply).
    jarJar("org.jetbrains.kotlin:kotlin-stdlib:[2.0.0,3.0)") {
        isTransitive = false
    }
    implementation(kotlin("stdlib"))
    "additionalRuntimeClasspath"("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")

    // Kotlin coroutines
    jarJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:[1.8.0,2.0)") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    "additionalRuntimeClasspath"("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Kotlin reflect
    jarJar("org.jetbrains.kotlin:kotlin-reflect:[2.0.0,3.0)") {
        isTransitive = false
    }
    implementation(kotlin("reflect"))
    "additionalRuntimeClasspath"("org.jetbrains.kotlin:kotlin-reflect:2.1.0")

    // LuaJ also needs to be on the dev run classpath (same reason as Kotlin)
    "additionalRuntimeClasspath"("org.luaj:luaj-jse:3.0.1")

    // JSON handling (Minecraft already has Gson, no need to bundle)
    implementation("com.google.code.gson:gson:2.10.1")
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
