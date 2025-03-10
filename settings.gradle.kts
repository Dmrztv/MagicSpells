rootProject.name = "MagicSpellsParent"

include("core")
include("factions")
include("memory")
include("shop")
include("teams")
include("towny")

include(":nms:shared")
include(":nms:v1_21")
include(":nms:v1_21_1")

startParameter.isParallelProjectExecutionEnabled = true

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

