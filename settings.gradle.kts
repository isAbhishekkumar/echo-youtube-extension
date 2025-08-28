pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        jcenter() // Add JCenter for older dependencies
        maven { url = uri("https://jitpack.io") } // Add JitPack for GitHub-hosted dependencies
    }
}

rootProject.name = "Youtube Music Extension"
include(":app")
include(":ext")
