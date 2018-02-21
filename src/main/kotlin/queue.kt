import org.jetbrains.teamcity.rest.ProjectId

fun main(args: Array<String>) {
    reportQueue("ijplatform_master_Idea")
    reportQueue("ijplatform_master_Idea_Tests")
    reportQueue("ijplatform_master_CIDR_CLion")
    reportQueue()
}

private fun reportQueue(projectId: String? = null) {
    printStatistics((projectId ?: "root") + "_queue", queueSize(projectId).toString())
}

private fun queueSize(projectId: String? = null) = teamcity.queuedBuilds(projectId?.let { ProjectId(projectId) }).size
