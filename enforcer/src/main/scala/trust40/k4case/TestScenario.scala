package trust40.k4case

import java.time.LocalDateTime

import trust40.enforcer.tcof._

case class TestScenarioSpec(
                             workersPerWorkplaceCount: Int,
                             workersOnStandbyCount: Int,
                             startTime: LocalDateTime
                           )

case class Position(x: Double, y: Double)
case class Area(left: Double, top: Double, right: Double, bottom: Double) {
  def this(topLeft: Position, bottomRight: Position) = this(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
  def contains(pos: Position): Boolean = pos != null && pos.x >= left && pos.y >= top && pos.x <= right && pos.y <= bottom
}

case class ScenarioEvent(timestamp: LocalDateTime, eventType: String, person: String, position: Position)

trait WithId {
  def id: String
}

class TestScenario(scenarioParams: TestScenarioSpec) extends Model with ModelGenerator {

  val startTimestamp = scenarioParams.startTime
  var now = startTimestamp

  case class WorkerPotentiallyLateNotification(shift: Shift, worker: Worker) extends Notification

  case class AssignmentCanceledNotification(shift: Shift) extends Notification
  case class StandbyNotification(shift: Shift) extends Notification
  case class WorkerReplacedNotification(shift: Shift, worker: Worker) extends Notification

  class Door(
              val id: String,
            ) extends Component with WithId {
    name(s"Door ${id}")

    override def toString = s"Door($id)"
  }

  class Dispenser(
                   val id: String,
                 ) extends Component with WithId {
    name(s"Protection equipment dispenser ${id}")

    override def toString = s"Dispenser($id)"
  }

  class Worker(
                val id: String,
                var position: Position,
                val capabilities: Set[String],
                var hasHeadGear: Boolean
              ) extends Component with WithId {
    name(s"Worker ${id}")

    override def toString = s"Worker($id, $position)"

    def isAt(room: Room) = room.area.contains(position)
  }

  abstract class Room(
              val id: String,
              val area: Area,
              val entryDoor: Door
            ) extends Component with WithId {
    name(s"Room ${id}")
  }

  class WorkPlace(
                   id: String,
                   area: Area,
                   entryDoor: Door
                 ) extends Room(id, area, entryDoor) {
    name(s"WorkPlace ${id}")

    var factory: Factory = _

    override def toString = s"WorkPlace($id)"
  }

  class Factory(
                 id: String,
                 area: Area,
                 entryDoor: Door,
                 val dispenser: Dispenser,
                 val workPlaces: List[WorkPlace]
               ) extends Room(id, area, entryDoor) {
    name(s"Factory ${id}")

    for (workPlace <- workPlaces) {
      workPlace.factory = this
    }

    override def toString = s"Factory($id)"
  }

  class Shift(
               val id: String,
               val startTime: LocalDateTime,
               val endTime: LocalDateTime,
               val workPlace: WorkPlace,
               val foreman: Worker,
               val workers: List[Worker],
               val standbys: List[Worker],
               val assignments: Map[Worker, String]
             ) extends Component with WithId {
    name(s"Shift ${id}")

    override def toString = s"Shift($startTime, $endTime, $workPlace, $foreman, $workers, $standbys, $assignments)"
  }


  import ModelDSL._
  val (factory, workersMap, shiftsMap) = withModel { implicit builder =>
    for (wp <- List("A", "B", "C")) {
      val foremanId = s"$wp-foreman"
      withWorker(foremanId, Set("A", "B", "C", "D", "E"))

      val workersInShift = (1 to scenarioParams.workersPerWorkplaceCount).map(idx => f"$wp%s-worker-$idx%03d")
      for (id <- workersInShift) {
        withWorker(id, Set("A", "B", "C", "D", "E"))
      }

      val workersOnStandby = (1 to scenarioParams.workersOnStandbyCount).map(idx => f"standby-$idx%03d")

      for (id <- workersOnStandby) {
        withWorker(id, Set("A", "B", "C", "D", "E"))
      }

      withShift(
        wp,
        startTimestamp plusHours 1,
        startTimestamp plusHours 9,
        wp,
        foremanId,
        workersInShift.toList,
        workersOnStandby.toList,
        workersInShift.map(wrk => (wrk, "A")).toMap
      )
    }
  }


  class FactoryTeam(factory: Factory) extends RootEnsemble {
    name(s"Factory team ${factory.id}")

    class ShiftTeam(shift: Shift) extends Ensemble {
      name(s"Shift team ${shift.id}")

      // These are like invariants at a given point of time
      val calledInStandbys = shift.standbys.filter(wrk => wrk notified StandbyNotification(shift))
      val availableStandbys = shift.standbys diff calledInStandbys

      val canceledWorkers = shift.workers.filter(wrk => wrk notified AssignmentCanceledNotification(shift))
      val canceledWorkersWithoutStandby = canceledWorkers.filterNot(wrk => shift.foreman notified WorkerReplacedNotification(shift, wrk))

      val assignedWorkers = (shift.workers union calledInStandbys) diff canceledWorkers


      object AccessToFactory extends Ensemble { // Kdyz se constraints vyhodnoti na LogicalBoolean, tak ten ensemble vubec nezatahujeme solver modelu a poznamename si, jestli vysel nebo ne
        name(s"AccessToFactory")

        situation {
          (now isAfter (shift.startTime minusMinutes 30)) &&
            (now isBefore (shift.endTime plusMinutes 30))
        }

        allow(shift.foreman, "enter", shift.workPlace.factory)
        allow(assignedWorkers, "enter", shift.workPlace.factory)
      }

      object AccessToDispenser extends Ensemble {
        name(s"AccessToDispenser")

        situation {
          (now isAfter (shift.startTime minusMinutes 15)) &&
            (now isBefore shift.endTime)
        }

        allow(assignedWorkers, "use", shift.workPlace.factory.dispenser)
      }

      object AccessToWorkplace extends Ensemble { // Kdyz se constraints vyhodnoti na LogicalBoolean, tak ten ensemble vubec nezatahujeme solver modelu a poznamename si, jestli vysel nebo ne
        name(s"AccessToWorkplace")

        val workersWithHeadGear = (shift.foreman :: assignedWorkers).filter(wrk => wrk.hasHeadGear)

        situation {
          (now isAfter (shift.startTime minusMinutes 30)) &&
            (now isBefore (shift.endTime plusMinutes 30))
        }

        allow(workersWithHeadGear, "enter", shift.workPlace)
      }



      object NotificationAboutWorkersThatArePotentiallyLate extends Ensemble {
        name(s"NotificationAboutWorkersThatArePotentiallyLate")

        val workersThatAreLate = assignedWorkers.filter(wrk => !(wrk isAt shift.workPlace.factory))

        situation {
          now isAfter (shift.startTime minusMinutes 20)
        }

        workersThatAreLate.foreach(wrk => notify(shift.foreman, WorkerPotentiallyLateNotification(shift, wrk)))

        allow(shift.foreman, "read.personalData.phoneNo", workersThatAreLate)
        allow(shift.foreman, "read.distanceToWorkPlace", workersThatAreLate)
      }


      object CancellationOfWorkersThatAreLate extends Ensemble {
        name(s"CancellationOfWorkersThatAreLate")

        val workersThatAreLate = assignedWorkers.filter(wrk => !(wrk isAt shift.workPlace.factory))

        situation {
          now isAfter (shift.startTime minusMinutes 15)
        }

        notify(workersThatAreLate, AssignmentCanceledNotification(shift))
      }

      object AssignmentOfStandbys extends Ensemble {
        name(s"AssignmentOfStandbys")

        class StandbyAssignment(canceledWorker: Worker) extends Ensemble {
          name(s"StandbyAssignment for ${canceledWorker.id}")

          val standby = oneOf(availableStandbys)

          constraints {
            standby.all(_.capabilities contains shift.assignments(canceledWorker))
          }
        }

        val standbyAssignments = rules(canceledWorkersWithoutStandby.map(new StandbyAssignment(_)))

        val selectedStandbys = unionOf(standbyAssignments.map(_.standby))

        situation {
          (now isAfter (shift.startTime minusMinutes 15)) &&
          (now isBefore shift.endTime)
        }

        constraints {
          standbyAssignments.map(_.standby).allDisjoint
        }

        notify(selectedStandbys.selectedMembers, StandbyNotification(shift))
        canceledWorkersWithoutStandby.foreach(wrk => notify(shift.foreman, WorkerReplacedNotification(shift, wrk)))
      }

      object NoAccessToPersonalDataExceptForLateWorkers extends Ensemble {
          name(s"NoAccessToPersonalDataExceptForLateWorkers")

          val workersPotentiallyLate =
              if ((now isAfter (shift.startTime minusMinutes 20)) && (now isBefore shift.startTime))
                  assignedWorkers.filter(wrk => !(wrk isAt shift.workPlace.factory))
              else
                  Nil

          val workers = shift.workers diff workersPotentiallyLate

          deny(shift.foreman, "read.personalData", workers, PrivacyLevel.ANY)
          deny(shift.foreman, "read.personalData", workersPotentiallyLate, PrivacyLevel.SENSITIVE)
      }

      rules(
        // Grants
        AccessToFactory,
        AccessToDispenser,
        AccessToWorkplace,
        NotificationAboutWorkersThatArePotentiallyLate,
        CancellationOfWorkersThatAreLate,
        AssignmentOfStandbys,

        // Assertions
        NoAccessToPersonalDataExceptForLateWorkers
      )
    }

    val shiftTeams = rules(shiftsMap.values.filter(shift => shift.workPlace.factory == factory).map(shift => new ShiftTeam(shift)))

    constraints {
      shiftTeams.map(shift => shift.AssignmentOfStandbys.selectedStandbys).allDisjoint
    }
  }

  val factoryTeam = root(new FactoryTeam(factory))
}

object TestScenario {
  def createScenarioSpec(workersPerWorkplaceCount: Int, workersOnStandbyCount: Int, startTime: LocalDateTime) = {
    TestScenarioSpec(
      workersPerWorkplaceCount = workersPerWorkplaceCount,
      workersOnStandbyCount = workersOnStandbyCount,
      startTime = startTime
    )
  }
}
