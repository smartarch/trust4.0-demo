package trust40.k4case

import java.time.LocalDateTime

import akka.actor.{Actor, Props}
import akka.event.Logging
import trust40.k4case.Enforcer.ResolverResult

import scala.util.control.Breaks.break

object Resolver {
  def props(scenario: TestScenario) = Props(new Resolver(scenario))

  final case class Resolve(currentTime: LocalDateTime, events: List[ScenarioEvent])
}

class Resolver(val scenario: TestScenario) extends Actor {
  import Resolver._

  private val log = Logging(context.system, this)

  private val solverLimitTime = 60000000000L

  private def resolve(currentTime: LocalDateTime, events: List[ScenarioEvent]): Unit = {
    log.info("Resolver started")
    log.info("Time: " + currentTime)
    log.info("Events: " + events)

    for (event <- events) {
      val worker = scenario.workersMap(event.person)
      worker.position = event.position

      if (event.eventType == "access-dispenser") {
        worker.hasHeadGear = true
      }
    }

    for (factoryTeam <- scenario.factoryTeams) {
      factoryTeam.init()
      factoryTeam.solverLimitTime(solverLimitTime)
      factoryTeam.solve()

      if (factoryTeam.exists) {
        // log.info("Utility: " + shiftTeams.instance.solutionUtility)
        // log.info(shiftTeams.instance.toString)

        factoryTeam.commit()

        // for (action <- shiftTeams.actions) {
        //  log.info(action)
        // }

      } else {

        log.error("Error. No solution exists.")
        break()
      }
    }

    log.info("Resolver finished")
  }

  def receive = {
    case Resolve(currentTime, events) =>
      resolve(currentTime, events)
      sender() ! ResolverResult()
  }

}

