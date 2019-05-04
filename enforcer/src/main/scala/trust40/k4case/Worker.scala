package trust40.k4case

import java.time.temporal.ChronoUnit
import java.time.{Duration, LocalDateTime}

import akka.actor.{Actor, Props}
import akka.event.Logging
import trust40.k4case.Simulation.{Events, Reset, Step}

import scala.collection.mutable
import scala.util.Random

abstract class AbstractSimulatedWorker(val person: String, val startTime: LocalDateTime) extends Actor {
  protected val log = Logging(context.system, this)
  def startPosition = Position(0, 90)

  private abstract class Action(val startTime: LocalDateTime, val duration: Duration, val startPosition: Position, val targetPosition: Position) {
    def getEvents(currentTime: LocalDateTime): List[ScenarioEvent] = List()
    def isDue(currentTime: LocalDateTime) = !currentTime.isBefore(startTime)
    def isOver(currentTime: LocalDateTime) = !currentTime.isBefore(startTime.plus(duration))
  }

  private class InitAction() extends Action(startTime, Duration.ZERO, startPosition, startPosition) {
    override def toString: String = s"InitAction($startTime, $startPosition)"
    override def getEvents(currentTimestamp: LocalDateTime): List[ScenarioEvent] = List(ScenarioEvent(startTime, "init", person, startPosition))
  }

  private class MoveAction(startTime: LocalDateTime, duration: Duration, startPosition: Position, targetPosition: Position) extends Action(startTime, duration, startPosition, targetPosition) {
    override def toString: String = s"MoveAction($startTime, $duration, $startPosition, $targetPosition)"
    override def getEvents(currentTimestamp: LocalDateTime): List[ScenarioEvent] = {
      var nsSinceStart = ChronoUnit.NANOS.between(startTime, currentTimestamp)
      val nsTotal = duration.toNanos

      if (nsSinceStart > nsTotal) {
        nsSinceStart = nsTotal
      }

      val alpha = nsSinceStart.toDouble / nsTotal

      val x = startPosition.x * (1 - alpha) + targetPosition.x * alpha
      val y = startPosition.y * (1 - alpha) + targetPosition.y * alpha

      List(ScenarioEvent(startTime.plusNanos(nsSinceStart), "move", person, Position(x, y)))
    }
  }

  private class AccessDoorAction(timestamp: LocalDateTime, doorPosition: Position) extends Action(timestamp, Duration.ZERO, doorPosition, doorPosition) {
    override def toString: String = s"AccessDoorAction($timestamp, $doorPosition)"
    override def getEvents(currentTimestamp: LocalDateTime): List[ScenarioEvent] = List(ScenarioEvent(timestamp, "access-door", person, doorPosition))
  }

  private class AccessDispenserAction(timestamp: LocalDateTime, dispenserPosition: Position) extends Action(timestamp, Duration.ZERO, dispenserPosition, dispenserPosition) {
    override def toString: String = s"AccessDispenserAction($timestamp, $dispenserPosition)"
    override def getEvents(currentTimestamp: LocalDateTime): List[ScenarioEvent] = List(ScenarioEvent(timestamp, "access-dispenser", person, dispenserPosition))
  }

  private class WaitAction(timestamp: LocalDateTime, duration: Duration, position: Position) extends Action(timestamp, duration, position, position) {
    override def toString: String = s"WaitAction($timestamp, $duration, $position)"
  }

  private val futureActions = mutable.ListBuffer.empty[Action]
  private var currentAction: Action = _

  protected var currentTime: LocalDateTime = _
  protected var currentPosition: Position = _

  protected var currentEpoch: Int = _

  private def generateEvents(time: LocalDateTime): List[ScenarioEvent] = {
    val events = mutable.ListBuffer.empty[ScenarioEvent]

    def addEventsAndDropAction(): Unit = {
      events ++= currentAction.getEvents(time)

      if (futureActions.isEmpty) {
        generateActions()
      }

      currentAction = null
    }


    if (currentAction == null && futureActions.isEmpty) {
      currentTime = time
      generateActions()
    }

    if (currentAction != null && currentAction.isOver(time)) {
      addEventsAndDropAction()
    }

    while (futureActions.nonEmpty && currentAction == null) {
      currentAction = futureActions.head
      currentTime = currentAction.startTime.plus(currentAction.duration)
      currentPosition = currentAction.targetPosition

      futureActions.remove(0)

      if (currentAction.isDue(time) && currentAction.isOver(time)) {
        addEventsAndDropAction()
      }
    }

    if (currentAction != null && currentAction.isDue(time)) {
      events ++= currentAction.getEvents(time)
    }

    events.toList
  }

  private val random = new Random(42)
  private def lastActionTime = if (futureActions.nonEmpty) futureActions.last.startTime.plus(futureActions.last.duration) else currentTime
  private def lastActionPosition = if (futureActions.nonEmpty) futureActions.last.targetPosition else currentPosition

  protected def move(x: Int, y: Int, maxPace: Double = 10 /* seconds / position unit */, minPace: Double = 12): Unit = {
    val startPosition = lastActionPosition
    val xDistance = startPosition.x - x
    val yDistance = startPosition.y - y
    val distance = Math.sqrt(xDistance * xDistance + yDistance * yDistance)
    val duration = Duration.ofMillis((distance * (maxPace + random.nextDouble() * (minPace - maxPace)) * 1000).toInt)
    val targetPosition = Position(x, y)

    futureActions += new MoveAction(lastActionTime, duration, startPosition, targetPosition)
  }

  protected def accessDoor(): Unit = futureActions += new AccessDoorAction(lastActionTime, lastActionPosition)

  protected def accessDispenser(): Unit = futureActions += new AccessDoorAction(lastActionTime, lastActionPosition)

  protected def waitRandom(minDuration: Duration, maxDuration: Duration): Unit = {
    val duration = minDuration plusSeconds random.nextInt((maxDuration minus minDuration).getSeconds().asInstanceOf[Int])
    futureActions += new WaitAction(lastActionTime, duration, lastActionPosition)
  }

  protected def waitTillAfterStart(durationFromStart: Duration): Unit = {
    val startTime = lastActionTime
    val endTime = this.startTime plus durationFromStart
    assert(startTime.isBefore(endTime))
    val duration = Duration.between(startTime, endTime)

    futureActions += new WaitAction(startTime, duration, lastActionPosition)
  }

  protected implicit class EventDuration(value: Int) {
    def seconds = Duration.ofSeconds(value)
    def minutes = Duration.ofMinutes(value)
    def hours = Duration.ofHours(value)
  }

  protected def generateActions() {}
  protected def generateInitialActions() {}

  private def processStep(currentTime: LocalDateTime): Unit = {
    val events = generateEvents(currentTime)
    context.parent ! Events(currentEpoch, events)
  }

  private def processReset(epoch: Int): Unit = {
    currentEpoch = epoch

    futureActions.clear()
    currentAction = null

    currentTime = startTime
    currentPosition = startPosition

    futureActions += new InitAction()
    generateInitialActions()
  }

  def receive = {
    case Step(currentTime) => processStep(currentTime)
    case Reset(epoch) => processReset(epoch)
  }
}


object SimulatedWorkerInShiftA {
  def props(person: String, startTime: LocalDateTime) = Props(new SimulatedWorkerInShiftA(person, startTime))
}

class SimulatedWorkerInShiftA(person: String, startTime: LocalDateTime) extends AbstractSimulatedWorker(person, startTime) {
  override protected def generateInitialActions(): Unit = {
    waitRandom(29 minutes, 39 minutes)

    move(20, 90)
    accessDoor()
    move(30, 90)
    accessDispenser()
    move(30, 50)
    move(40, 50)
    accessDoor()
    move(50, 50)

    waitTillAfterStart(9 hours)

    waitRandom(2 minutes, 5 minutes)
    move(40, 50)
    accessDoor()
    move(30, 50)
    move(30, 90)
    move(20, 90)
    accessDoor()
    move(0, 90)
  }
}


object SimulatedWorkerInShiftB {
  def props(person: String, startTime: LocalDateTime) = Props(new SimulatedWorkerInShiftB(person, startTime))
}

class SimulatedWorkerInShiftB(person: String, startTime: LocalDateTime) extends AbstractSimulatedWorker(person, startTime) {
  override protected def generateInitialActions(): Unit = {
    waitRandom(23 minutes, 33 minutes)

    move(20, 90)
    accessDoor()
    move(30, 90)
    accessDispenser()
    move(110, 90)
    move(110, 50)
    move(120, 50)
    accessDoor()
    move(130, 50)

    waitTillAfterStart(9 hours)

    waitRandom(2 minutes, 5 minutes)
    move(120, 50)
    accessDoor()
    move(110, 50)
    move(110, 90)
    move(20, 90)
    accessDoor()
    move(0, 90)
  }
}


object SimulatedWorkerInShiftC {
  def props(person: String, startTime: LocalDateTime) = Props(new SimulatedWorkerInShiftC(person, startTime))
}

class SimulatedWorkerInShiftC(person: String, startTime: LocalDateTime) extends AbstractSimulatedWorker(person, startTime) {
  override protected def generateInitialActions(): Unit = {
    waitRandom(25 minutes, 35 minutes)

    move(20, 90)
    accessDoor()
    move(30, 90)
    accessDispenser()
    move(110, 90)
    move(110, 100)
    move(120, 100)
    accessDoor()
    move(130, 100)

    waitTillAfterStart(9 hours)

    waitRandom(2 minutes, 5 minutes)
    move(120, 100)
    accessDoor()
    move(110, 100)
    move(110, 90)
    move(20, 90)
    accessDoor()
    move(0, 90)
  }
}
