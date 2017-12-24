import com.google.common.math.Quantiles
import org.jetbrains.teamcity.rest.*

private val teamcity = TeamCityInstanceFactory.httpAuth(
        System.getProperty("teamcity") ?: System.getenv("teamcity"),
        System.getProperty("user") ?: System.getenv("user"),
        System.getProperty("password") ?: System.getenv("password"))

private val format = "%.0f"

fun main(args: Array<String>) {
    val allDuration = mutableListOf<Long>()
    val buildDuration = mutableListOf<Long>()
    val queueDuration = mutableListOf<Long>()

    val builds = testConfigurations().flatMap { configurationBuilds(it.id) }

    for (b in builds) {
        val q = b.fetchQueuedDate().time
        val s = b.fetchStartDate().time
        val f = b.fetchFinishDate().time

        allDuration.add(f - q)
        buildDuration.add(f - s)
        queueDuration.add(s - q)
    }

    calcStatistics("Total time", allDuration)
    calcStatistics("Build time", buildDuration)
    calcStatistics("Queue time", queueDuration)
}

private fun calcStatistics(prefix: String, diff: List<Long>) {
    printStatistics(prefix + ": max, ms", format.format(diff.max()!! * 1.0))
    printStatistics(prefix + ": min, ms", format.format(diff.min()!! * 1.0))
    printStatistics(prefix + ": avg, ms", format.format(diff.average()))
    for (i in arrayOf(90, 80, 70, 60, 50)) {
        printStatistics(prefix + ": ${i}th percentile, ms", format.format(Quantiles.percentiles().index(i).compute(diff)))
    }
    println()
}

private fun configurationBuilds(id: BuildConfigurationId): List<Build> {
    val list = teamcity.builds().fromConfiguration(id).withAnyStatus().limitResults(40).list()
    println("${list.size} ${id.stringId}")
    return list
}

private val blackList = setOf(
        "ijplatform_master_Idea_Tests_RegressionTests",
        "ijplatform_master_Idea_Tests_Aggregator",
        "ijplatform_master_Idea_AllTests",
        "ijplatform_master_Idea_QlTests",
        "ijplatform_master_Idea_CommunityTestsCoverage"
)

private fun testConfigurations() =
        teamcity.project(ProjectId("ijplatform_master_Idea_Tests"))
                .fetchBuildConfigurations()
                .filter { !blackList.contains(it.id.stringId) }

private fun printStatistics(key: String, value: String) {
    println("##teamcity[buildStatisticValue key='$key' value='$value']")
}