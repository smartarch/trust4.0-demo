package trust40.k4case

import java.time.{Duration, LocalDateTime}

import scala.collection.mutable
import scala.util.Random

trait ModelGenerator {
  this: TestScenario =>

  object ModelDSL {

    private class ModelBuilder {
      val workersMap = mutable.Map.empty[String, Worker]
      val shiftsMap = mutable.Map.empty[String, Shift]

      val workplaces = for (wpId <- List("A", "B", "C"))
        yield new WorkPlace(
          s"workplace-$wpId",
          new Area(FactoryMap(s"Workplace-$wpId-TL"), FactoryMap(s"Workplace-$wpId-BR")),
          new Door(s"gate-$wpId")
        )

      val workPlacesMap = mutable.Map(workplaces.map(x => x.id -> x): _*)


      val factory = new Factory(
      "factory",
        new Area(FactoryMap("Factory-TL"), FactoryMap("Factory-BR")),
        new Door("main-gate"),
        new Dispenser("dispenser"),
        workplaces
      )

      def addWorker(id: String, caps: Set[String]): Unit = {
        workersMap(id) = new Worker(id, null, caps, false)
      }

      def addShift(id: String, startTime: LocalDateTime, endTime: LocalDateTime, workPlace: String, foreman: String, workers: List[String], standbys: List[String], assignments: Map[String, String]): Unit = {

        shiftsMap(id) = new Shift(
          id,
          startTime,
          endTime,
          workPlacesMap(workPlace),
          workersMap(foreman),
          workers.map(wrk => workersMap(wrk)),
          standbys.map(wrk => workersMap(wrk)),
          assignments.map(keyVal => (workersMap(keyVal._1) -> keyVal._2))
        )
      }
    }

    def withWorker(id: String, caps: Set[String])(implicit ms: ModelBuilder): Unit = ms.addWorker(id, caps)

    def withShift(id: String, startTime: LocalDateTime, endTime: LocalDateTime, workPlace: String, foreman: String, workers: List[String], standbys: List[String], assignments: Map[String, String])(implicit ms: ModelBuilder): Unit =
      ms.addShift(id, startTime, endTime, workPlace, foreman, workers, standbys, assignments)


    def withModel(ops: ModelBuilder => Unit): (Factory, Map[String, Worker], Map[String, Shift]) = {
      val builder = new ModelBuilder
      ops(builder)

      (builder.factory, builder.workersMap.toMap, builder.shiftsMap.toMap)
    }
  }
}

