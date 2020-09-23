rootProject.name = "ericsplugins"

include(":pestcontrol")
include(":mudrunecrafter")
include(":xptracker")
include(":tickfishing")
include(":powerskiller")
include(":pvphelper")
include(":nightmarezone")
include(":minnowsbot")
include(":gargoylefighter")
include(":fishingtrawler")
include(":cursealch")
include(":tickfishinghotkey")
include(":zulrah")
include(":motherloadmine")


for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "${name.toLowerCase()}.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}
