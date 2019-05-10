package trust40.k4case

import java.time.LocalDateTime

import akka.actor.{Actor, Props}
import akka.event.Logging
import trust40.enforcer.tcof.AllowAction
import trust40.k4case.Simulation.{Permissions, Reset}

import scala.collection.mutable


object Resolver {
  def props(scenarioSpec: TestScenarioSpec) = Props(new Resolver(scenarioSpec))

  final case class Resolve(currentTime: LocalDateTime, events: List[ScenarioEvent])
}

class Resolver(val scenarioSpec: TestScenarioSpec) extends Actor {
  import Resolver._

  private val log = Logging(context.system, this)

  private val solverLimitTime = 60000000000L

  private var scenario: TestScenario = _

  private var currentEpoch: Int = _


  private def processResolve(currentTime: LocalDateTime, events: List[ScenarioEvent]): Unit = {
    log.info("Resolver started")
    log.info("Time: " + currentTime)
    log.info("Events: " + events)

    scenario.now = currentTime

    for (event <- events) {
      val worker = scenario.workersMap(event.person)
      worker.position = event.position

      if (event.eventType == "access-dispenser") {
        worker.hasHeadGear = true
      }
    }

    val perms = mutable.ListBuffer.empty[(String, String, String)]

    val factoryTeam = scenario.factoryTeam
    factoryTeam.init()
    factoryTeam.solverLimitTime(solverLimitTime)
    factoryTeam.solve()

    if (factoryTeam.exists) {
      // log.info("Utility: " + shiftTeams.instance.solutionUtility)
      // log.info(shiftTeams.instance.toString)

      factoryTeam.commit()

      for (action <- factoryTeam.actions) {
        println(action)
        action match {
          case AllowAction(subj: WithId, action, obj: WithId) =>
            perms += ((subj.id, action, obj.id))
          case _ =>
        }
      }

      // call to KIT <- factoryTeam.actions ... => consistent set of action

    } else {
      log.error("Error. No solution exists.")
    }

    log.info("Resolver finished")

    sender() ! Permissions(currentEpoch, perms.toList)
  }

  private def processReset(epoch: Int): Unit = {
    currentEpoch = epoch
    scenario = new TestScenario(scenarioSpec)
  }

  def receive = {
    case Resolve(currentTime, events) =>
      processResolve(currentTime, events)

    case Reset(epoch: Int) =>
      processReset(epoch)
  }

}

