package trust40.k4case

import java.time.LocalDateTime

import trust40.enforcer.tcof._

case class TestScenarioSpec(
                             workersPerWorkplaceCount: Int,
                             workersOnStandbyCount: Int,
                             factoriesCount: Int,
                             startTs: String
                           )

case class Position(x: Double, y: Double)


case class ScenarioEvent(timestamp: LocalDateTime, eventType: String, person: String, position: Position)

class TestScenario(scenarioParams: TestScenarioSpec) extends Model with ModelGenerator {

  val startTimestamp = LocalDateTime.parse("2018-12-03T08:00:00")
  var now = startTimestamp

  case class WorkerPotentiallyLateNotification(shift: Shift, worker: Worker) extends Notification

  case class AssignmentCanceledNotification(shift: Shift) extends Notification
  case class StandbyNotification(shift: Shift) extends Notification
  case class WorkerReplacedNotification(shift: Shift, worker: Worker) extends Notification

  class Door(
              val id: String,
              val position: Position
            ) extends Component {
    name(s"Door ${id}")

    override def toString = s"Door($id, $position)"
  }

  class Dispenser(
                   val id: String,
                   val position: Position
                 ) extends Component {
    name(s"Protection equipment dispenser ${id}")

    override def toString = s"Dispenser($id, $position)"
  }

  class Worker(
                val id: String,
                var position: Position,
                val capabilities: Set[String],
                var hasHeadGear: Boolean
              ) extends Component {
    name(s"Worker ${id}")

    override def toString = s"Worker($id, $position, $capabilities)"

    def isAt(room: Room) = room.positions.contains(position)
  }

  abstract class Room(
              val id: String,
              val positions: List[Position],
              val entryDoor: Door
            ) extends Component {
    name(s"Room ${id}")
  }

  class WorkPlace(
                   id: String,
                   positions: List[Position],
                   entryDoor: Door
                 ) extends Room(id, positions, entryDoor) {
    name(s"WorkPlace ${id}")

    var factory: Factory = _

    override def toString = s"WorkPlace($id, $positions, $entryDoor)"
  }

  class Factory(
                 id: String,
                 positions: List[Position],
                 entryDoor: Door,
                 val dispenser: Dispenser,
                 val workPlaces: List[WorkPlace]
               ) extends Room(id, positions, entryDoor) {
    name(s"Factory ${id}")

    for (workPlace <- workPlaces) {
      workPlace.factory = this
    }

    override def toString = s"Factory($id, $positions, $entryDoor, $dispenser, $workPlaces)"
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
             ) extends Component {
    name(s"Shift ${id}")

    override def toString = s"Shift($startTime, $endTime, $workPlace, $foreman, $workers, $standbys, $assignments)"
  }


  val factoryIds = (1 to scenarioParams.factoriesCount).map(idx => f"factory$idx%02d")

  import ModelDSL._
  val (workersMap, factoriesMap, shiftsMap) = withModel { implicit builder =>
    for ((factoryId, factoryIdx) <- factoryIds.zipWithIndex) {
      withFactory(factoryId, 0, 0) { implicit scope =>
        for (wp <- List("A", "B", "C")) {
          val foremanId = s"$factoryId-$wp-foreman"
          withWorker(foremanId, Set("A", "B", "C", "D", "E"))

          val workersInShift = (1 to scenarioParams.workersPerWorkplaceCount).map(idx => f"$factoryId%s-$wp%s-worker-$idx%03d")
          for (id <- workersInShift) {
            withWorker(id, Set("A", "B", "C", "D", "E"))
          }

          val workersOnStandby = (1 to scenarioParams.workersOnStandbyCount).map(idx => f"$factoryId%s-standby-$idx%03d")

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

  val factoryTeams = factoriesMap.values.map(factory => root(new FactoryTeam(factory)))
}

object TestScenario {
  def createScenarioSpec(factoriesCount: Int, workersPerWorkplaceCount: Int, workersOnStandbyCount: Int) = {
    TestScenarioSpec(
      workersPerWorkplaceCount = workersPerWorkplaceCount,
      workersOnStandbyCount = workersOnStandbyCount,
      factoriesCount = factoriesCount,
      startTs = "2018-12-03T08:00:00"
    )
  }
}
