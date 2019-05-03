package trust40.k4case

import java.time.LocalDateTime

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import trust40.k4case.Resolver.Resolve

import scala.collection.mutable


object Enforcer {
  def props(resolver: ActorRef) = Props(new Enforcer(resolver))

  final case class Events(events: List[ScenarioEvent])
  final case class ResolverResult()
  final case class Step(currentTime: LocalDateTime)
}


class Enforcer(val resolver: ActorRef) extends Actor {
  import Enforcer._

  private val log = Logging(context.system, this)

  private var eventsSinceLastResolve = mutable.ListBuffer.empty[ScenarioEvent]
  private var resolving = false

  private var stepNo = 0
  private var resolveNeeded = false

  private def resolve(currentTime: LocalDateTime): Unit = {
    log.info("Scheduling resolver")
    val events = eventsSinceLastResolve.sortWith((ev1, ev2) => ev1.timestamp.isBefore(ev2.timestamp)).toList
    eventsSinceLastResolve.clear()
    resolver ! Resolve(currentTime, events)
  }

  def receive = {
    case Events(events) =>
      eventsSinceLastResolve ++= events

    case ResolverResult() =>
      log.info("Received resolver result")
      resolving = false

    case Step(currentTime) =>
      log.info(s"Step received: $currentTime")
      if (stepNo % 60 == 0) {
        resolveNeeded = true
      }

      stepNo += 1

      if (resolveNeeded && !resolving) {
        resolving = true
        resolveNeeded = false

        resolve(currentTime)
      }
  }

}

