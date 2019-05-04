package trust40.k4case

import java.time.format.DateTimeFormatter
import java.time.{Duration, LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.event.Logging

import scala.collection.mutable
import scala.concurrent.duration._

case class WorkerState(position: Position)
case class SimulationState(time: String, workers: Map[String, WorkerState], permissions: List[(String, String, String)])

object Simulation {
  def props() = Props(new Simulation())

  case object Play
  case object Pause
  case object Reset
  case object Status

  final case class Reset(epoch: Int)
  final case class Events(epoch: Int, events: List[ScenarioEvent])
  final case class Permissions(epoch: Int, permissions: List[(String, String, String)])
  final case class Step(currentTime: LocalDateTime)

  private case object TickTimer
  private case object Tick
}

class Simulation() extends Actor with Timers {
  import Simulation._

  object State extends Enumeration {
    type State = Value

    // This has to be aligned with visualizer/client/src/FactoryMap.js
    val START = Value(0)
    val PLAYING = Value(1)
    val PAUSED = Value(2)
    val END = Value(3)
  }

  import State._

  private val log = Logging(context.system, this)

  private var state = START
  private var currentTime: LocalDateTime = _

  private val startTime = LocalDateTime.parse("2018-12-03T08:00:00")
  private val endTime = startTime plus Duration.ofHours(10)

  private val scenarioSpec = TestScenario.createScenarioSpec(factoriesCount=1, workersPerWorkplaceCount=5, workersOnStandbyCount=2, startTime)

  private val resolver = context.actorOf(Resolver.props(scenarioSpec), name = "resolver")
  private val enforcer: ActorRef = context.actorOf(Enforcer.props(resolver), name = "enforcer")

  private var workers = mutable.ListBuffer.empty[ActorRef]
  for (factoryIdx <- 1 to scenarioSpec.factoriesCount) {
    val factoryId = f"factory$factoryIdx%02d"

    // foremen
    workers += context.actorOf(SimulatedWorkerInShiftA.props(s"$factoryId-A-foreman", startTime), name = s"$factoryId-A-foreman")
    workers += context.actorOf(SimulatedWorkerInShiftB.props(s"$factoryId-B-foreman", startTime), name = s"$factoryId-B-foreman")
    workers += context.actorOf(SimulatedWorkerInShiftC.props(s"$factoryId-C-foreman", startTime), name = s"$factoryId-C-foreman")

    // workers
    for (idx <- 1 to scenarioSpec.workersPerWorkplaceCount) {
      workers += context.actorOf(SimulatedWorkerInShiftA.props(f"$factoryId%s-A-worker-$idx%03d", startTime), name = f"$factoryId%s-A-worker-$idx%03d")
      workers += context.actorOf(SimulatedWorkerInShiftB.props(f"$factoryId%s-B-worker-$idx%03d", startTime), name = f"$factoryId%s-B-worker-$idx%03d")
      workers += context.actorOf(SimulatedWorkerInShiftC.props(f"$factoryId%s-C-worker-$idx%03d", startTime), name = f"$factoryId%s-C-worker-$idx%03d")
    }
  }


  private val workerStates = mutable.HashMap.empty[String, WorkerState]
  private var currentPermissions: List[(String, String, String)] = _

  private var currentEpoch = 0

  processReset()


  private def processReset(): Unit = {
    currentEpoch += 1

    state = START
    timers.cancel(TickTimer)

    currentTime = null
    workerStates.clear()
    currentPermissions = List()

    resolver ! Reset(currentEpoch)
    enforcer ! Reset(currentEpoch)

    for (worker <- workers) {
      worker ! Reset(currentEpoch)
    }

    processTick()
  }

  private def processPlay(): Unit = {
    if (state == START || state == PAUSED) {
      state = PLAYING
      timers.startPeriodicTimer(TickTimer, Tick, 100 millis)
    }
  }


  private def processPause(): Unit = {
    if (state == PLAYING) {
      state = PAUSED
      timers.cancel(TickTimer)
    }
  }

  private def processTick(): Unit = {
    if (currentTime == null) {
      currentTime = startTime
    } else {
      currentTime = currentTime plusSeconds(10)
    }

    for (worker <- workers) {
      worker ! Step(currentTime)
    }

    enforcer ! Step(currentTime)

    if (!currentTime.isBefore(endTime)) {
      state = END
      timers.cancel(TickTimer)
    }
  }

  private def processStatus(): Unit = {
    sender() ! SimulationState(currentTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), workerStates.toMap, currentPermissions)
  }

  private def processEvents(events: List[ScenarioEvent]): Unit = {
    for (event <- events) {
      workerStates(event.person) = WorkerState(event.position)
    }

    enforcer ! Enforcer.Events(events)
  }

  private def processPermissions(permissions: List[(String, String, String)]): Unit = {
    currentPermissions = permissions
  }


  def receive = {
    case Play => processPlay()
    case Pause => processPause()
    case Reset => processReset()
    case Status => processStatus()

    case Tick => processTick()

    case msg @ Events(epoch, events) =>
      if (epoch == currentEpoch) {
        processEvents(events)
      }

    case Permissions(epoch, permissions) =>
      if (epoch == currentEpoch) {
        processPermissions(permissions)
      }
  }
}

