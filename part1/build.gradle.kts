plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.simple)
}

application {
    // Specify the main class for the client application
    mainClass.set("eu.torvian.mcp.tutorials.part1.MainKt")
}

tasks.jar {
    archiveFileName.set("mcp-tutorials-part1.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "eu.torvian.mcp.tutorials.part1.MainKt"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        // Exclude problematic META-INF files
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/licenses/**") // Often good to exclude license duplicates too
        exclude("META-INF/AL2.0") // Another common duplicate, Apache License 2.0
        exclude("META-INF/LGPL2.1") // GNU Lesser General Public License 2.1

        // KEY FIX: Exclude module-info.class from different Java versions
        exclude("META-INF/versions/**/module-info.class")
        exclude("module-info.class") // Sometimes it's at the root or other places
    }

}