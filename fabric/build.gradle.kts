plugins {
    id("com.gradleup.shadow")
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    enableTransitiveAccessWideners.set(true)
    accessWidenerPath.set(project(":common").file("src/main/resources/ultrabeasts.accesswidener"))
    silentMojangMappingsLicense()
}

val shadowCommon: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")

    // API complete : ce mod utilise commands, lifecycle, player events,
    // entity events, networking, particles, rendering et object-builder.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    // needed for development launch targets to work
    modRuntimeOnly("org.graalvm.js:js:${property("graalvm_version")}")
    modRuntimeOnly("org.graalvm.sdk:graal-sdk:${property("graalvm_version")}")
    modRuntimeOnly("org.graalvm.regex:regex:${property("graalvm_version")}")
    modRuntimeOnly("org.graalvm.truffle:truffle-api:${property("graalvm_version")}")
    modRuntimeOnly("com.ibm.icu:icu4j:${property("icu4j_version")}")

    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin")}")
    modImplementation("com.cobblemon:fabric:${property("cobblemon_version")}") { isTransitive = false }

    implementation(project(":common", configuration = "namedElements"))
    "developmentFabric"(project(":common", configuration = "namedElements"))
    shadowCommon(project(":common", configuration = "transformProductionFabric"))
}

tasks {
    processResources {
        inputs.property("version", project.version)
        from(project(":common").file("src/main/resources/ultrabeasts.accesswidener"))
        filesMatching("fabric.mod.json") {
            expand(project.properties)
        }
    }

    jar {
        archiveBaseName.set("${rootProject.property("archives_base_name")}-${project.name}")
        archiveClassifier.set("dev-slim")
    }

    shadowJar {
        archiveClassifier.set("dev-shadow")
        archiveBaseName.set("${rootProject.property("archives_base_name")}-${project.name}")
        configurations = listOf(shadowCommon)
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        archiveBaseName.set("${rootProject.property("archives_base_name")}-${project.name}")
        archiveVersion.set("${rootProject.version}")
    }
}
