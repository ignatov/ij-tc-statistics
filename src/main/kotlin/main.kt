import com.google.common.math.Quantiles
import org.jetbrains.teamcity.rest.Build
import org.jetbrains.teamcity.rest.BuildConfigurationId
import org.jetbrains.teamcity.rest.ProjectId
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import java.util.*

private val teamcity = TeamCityInstanceFactory.httpAuth(
        property("teamcity"),
        property("user"),
        property("password"))

private fun property(key: String) = System.getProperty(key) ?: System.getenv(key)

private val format = "%.0f"

fun main(args: Array<String>) {
    val allDuration = mutableMapOf<String, Long>()
    val buildDuration = mutableMapOf<String, Long>()
    val queueDuration = mutableMapOf<String, Long>()

    val builds = testConfigurations().flatMap { configurationBuilds(it.id) }

    for (b in builds) {
        val q = b.fetchQueuedDate().time
        val s = b.fetchStartDate().time
        val f = b.fetchFinishDate().time

        allDuration.put(b.buildTypeId.stringId, f - q)
        buildDuration.put(b.buildTypeId.stringId, f - s)
        queueDuration.put(b.buildTypeId.stringId, s - q)
    }

    calcStatistics("Total time", allDuration)
    calcStatistics("Build time", buildDuration)
    calcStatistics("Queue time", queueDuration)
}

private fun calcStatistics(prefix: String, map: Map<String, Long>) {
    val top = map.entries.sortedBy({ entry -> -entry.value }).first()
    println("##teamcity[message text='The Worth $prefix for: ${top.key} is ${top.value}' status='WARNING']")

    val diff = map.values
    printStatistics(prefix + ": max, ms", format.format(diff.max()!! * 1.0))
    printStatistics(prefix + ": min, ms", format.format(diff.min()!! * 1.0))
    printStatistics(prefix + ": avg, ms", format.format(diff.average()))
    for (i in arrayOf(90, 80, 70, 60, 50)) {
        printStatistics(prefix + ": ${i}th percentile, ms", format.format(Quantiles.percentiles().index(i).compute(diff)))
    }
}

private fun configurationBuilds(id: BuildConfigurationId): List<Build> {
    val yesterday = GregorianCalendar()
    yesterday.add(Calendar.DAY_OF_MONTH, -1)

    val list = teamcity.builds().fromConfiguration(id).withAnyStatus().sinceDate(yesterday.time).list()
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