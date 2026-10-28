plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

architectury {
    common("neoforge", "fabric")
}

loom {
    silentMojangMappingsLicense()
    accessWidenerPath.set(file("src/main/resources/ultrabeasts.accesswidener"))
    mixin {
        // Must match "refmap" in ultrabeasts.mixins.json. The refmap maps the mixin targets
        // to intermediary names, which only Fabric uses at runtime: the NeoForge build strips
        // it from its jar and disables it in dev (see neoforge/build.gradle.kts).
        defaultRefmapName.set("ultrabeasts-refmap.json")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("com.cobblemon:mod:${property("cobblemon_version")}") { isTransitive = false }

    // Annotations and refmap generation for the camera and entity-tracking mixins.
    compileOnly("org.spongepowered:mixin:0.8.5")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit_version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit_version")}")
}