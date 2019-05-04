package trust40.k4case

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import trust40.k4case.Resolver.Resolve

import scala.collection.mutable

import Simulation.{Step, Reset, Permissions}

object Enforcer {
  def props(resolver: ActorRef) = Props(new Enforcer(resolver))

  final case class Events(events: List[ScenarioEvent])
}


class Enforcer(val resolver: ActorRef) extends Actor {
  import Enforcer._

  private val log = Logging(context.system, this)

  private var eventsSinceLastResolve = mutable.ListBuffer.empty[ScenarioEvent]
  private var resolving: Boolean = _
  private var resolvingCurrentTime: LocalDateTime = _

  private var nextStepNo: Int = _
  private var resolveNeeded: Boolean = _

  private var currentEpoch: Int = _
  private var currentPermissions: List[(String, String, String)] = _

  private def processReset(epoch: Int): Unit = {
    currentEpoch = epoch

    eventsSinceLastResolve.clear()
    resolving = false
    resolvingCurrentTime = null
    resolveNeeded = false
    nextStepNo = 0

    currentPermissions = List()
  }

  private def processEvents(events: List[ScenarioEvent]): Unit = {
    eventsSinceLastResolve ++= events
  }

  private def processPermissions(permissions: List[(String, String, String)]): Unit = {
    resolving = false
    currentPermissions = permissions

    context.parent ! Permissions(currentEpoch, permissions)
  }

  private def processStep(currentTime: LocalDateTime): Unit = {
    val stepNo = nextStepNo
    nextStepNo += 1

    if (stepNo % 60 == 0) {
      resolveNeeded = true
    }

    if (resolveNeeded && !resolving) {
      resolving = true
      resolvingCurrentTime = currentTime
      resolveNeeded = false

      val events = eventsSinceLastResolve.sortWith((ev1, ev2) => ev1.timestamp.isBefore(ev2.timestamp)).toList
      eventsSinceLastResolve.clear()
      resolver ! Resolve(currentTime, events)
    }
  }

  def receive = {
    case Reset(epoch) => processReset(epoch)
    case Events(events) => processEvents(events)
    case Step(currentTime) => processStep(currentTime)

    case Permissions(epoch, permissions) =>
      if (currentEpoch == epoch) processPermissions(permissions)
  }

}
