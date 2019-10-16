package trust40.k4case

import java.time.LocalDateTime

import akka.actor.{Actor, Props}
import akka.event.Logging
import trust40.enforcer.tcof.{AllowAction, NotifyAction}
import trust40.k4case.Simulation.{Reset}

import scala.collection.mutable

abstract class Permission
case class AllowPermission(subj: String, verb: String, obj: String) extends Permission
case class ComponentNotification(subj: String, action: String, params: List[String])

object Resolver {
  def props(scenarioSpec: TestScenarioSpec) = Props(new Resolver(scenarioSpec))

  final case class Resolve(currentTime: LocalDateTime, events: List[ScenarioEvent])
  final case class ResolverResult(epoch: Int, permissions: List[Permission], notifications: List[ComponentNotification])
}

class Resolver(val scenarioSpec: TestScenarioSpec) extends Actor {
  import Resolver._

  private val log = Logging(context.system, this)

  private val solverLimitTime = 60000000000L

  private var scenario: TestScenario = _

  private var currentEpoch: Int = _


  private def processResolve(currentTime: LocalDateTime, events: List[ScenarioEvent]): Unit = {
    // log.info("Resolver started")
    // log.info("Time: " + currentTime)
    // log.info("Events: " + events)

    scenario.now = currentTime

    for (event <- events) {
      val worker = scenario.workersMap(event.person)
      worker.position = event.position

      if (event.eventType == "access-dispenser") {
        worker.hasHeadGear = true
      }
    }

    val perms = mutable.ListBuffer.empty[Permission]
    val notifs = mutable.ListBuffer.empty[ComponentNotification]

    val factoryTeam = scenario.factoryTeam
    factoryTeam.init()
    factoryTeam.solverLimitTime(solverLimitTime)
    factoryTeam.solve()

    if (factoryTeam.exists) {
      // log.info("Utility: " + shiftTeams.instance.solutionUtility)
      // log.info(shiftTeams.instance.toString)

      factoryTeam.commit()

      for (action <- factoryTeam.actions) {
        // println(action)
        action match {
          case AllowAction(subj: WithId, verb, obj: WithId) =>
            perms += AllowPermission(subj.id, verb.toString, obj.id)
          case NotifyAction(subj: WithId, notif) =>
            notifs += ComponentNotification(subj.id, notif.getType, notif.getParams)
          case _ =>
        }
      }

      // call to KIT <- factoryTeam.actions ... => consistent set of action

    } else {
      log.error("Error. No solution exists.")
    }

    // log.info("Resolver finished")

    sender() ! ResolverResult(currentEpoch, perms.toList, notifs.toList)
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

