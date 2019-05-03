package trust40.k4case

import java.time.{Duration, LocalDateTime}

import scala.collection.mutable
import scala.util.Random

trait ModelGenerator {
  this: TestScenario =>

  object ModelDSL {

    private class ModelBuilder {
      val workersMap = mutable.Map.empty[String, Worker]
      val factoriesMap = mutable.Map.empty[String, Factory]
      val shiftsMap = mutable.Map.empty[String, Shift]
      val workPlacesMap = mutable.Map.empty[String, WorkPlace]
    }

    private class FactoryScope(val builder: ModelBuilder, val factoryId: String, val offsetX: Int, val offsetY: Int) {
      private def init(): Unit = {
        val positions = for {
          x <- (30 + offsetX).to(170 + offsetX, 10)
          y <- (10 + offsetY).to(110 + offsetY, 10)
        } yield Position(x, y)

        val workplaces = List(
          new WorkPlace(
            factoryId + "-A",
            List(Position(50 + offsetX, 50 + offsetY)),
            new Door(factoryId + "-door", Position(40 + offsetX, 50 + offsetY))
          ),
          new WorkPlace(
            factoryId + "-B",
            List(Position(130 + offsetX, 50 + offsetY)),
            new Door(factoryId + "-door", Position(120 + offsetX, 50 + offsetY))
          ),
          new WorkPlace(
            factoryId + "-C",
            List(Position(130 + offsetX, 100 + offsetY)),
            new Door(factoryId + "-door", Position(120 + offsetX, 100 + offsetY))
          )
        )

        builder.workPlacesMap ++= workplaces.map(wp => (wp.id, wp))

        builder.factoriesMap(factoryId) = new Factory(
          factoryId,
          positions.toList,
          new Door(factoryId + "-door", Position(20 + offsetX, 90 + offsetY)),
          new Dispenser(factoryId + "-dispenser", Position(30 + offsetX, 90 + offsetY)),
          workplaces
        )
      }

      def addWorker(id: String, caps: Set[String]): Unit = {
        builder.workersMap(id) = new Worker(id, Position(0 + offsetX, 90 + offsetY), caps, false)
      }

      def addShift(id: String, startTime: LocalDateTime, endTime: LocalDateTime, workPlace: String, foreman: String, workers: List[String], standbys: List[String], assignments: Map[String, String]): Unit = {
        val gid = factoryId + "-" + id

        builder.shiftsMap(gid) = new Shift(
          gid,
          startTime,
          endTime,
          builder.workPlacesMap(factoryId + "-" + workPlace),
          builder.workersMap(foreman),
          workers.map(wrk => builder.workersMap(wrk)),
          standbys.map(wrk => builder.workersMap(wrk)),
          assignments.map(keyVal => (builder.workersMap(keyVal._1) -> keyVal._2))
        )
      }

      init()
    }


    def withFactory(id: String, offsetX: Int, offsetY: Int)(ops: FactoryScope => Unit)(implicit builder: ModelBuilder): Unit = {
      val ms = new FactoryScope(builder, id, offsetX, offsetY)
      ops(ms)
    }

    def withWorker(id: String, caps: Set[String])(implicit ms: FactoryScope): Unit = ms.addWorker(id, caps)

    def withShift(id: String, startTime: LocalDateTime, endTime: LocalDateTime, workPlace: String, foreman: String, workers: List[String], standbys: List[String], assignments: Map[String, String])(implicit ms: FactoryScope): Unit =
      ms.addShift(id, startTime, endTime, workPlace, foreman, workers, standbys, assignments)


    def withModel(ops: ModelBuilder => Unit): (Map[String, Worker], Map[String, Factory], Map[String, Shift]) = {
      val builder = new ModelBuilder
      ops(builder)

      (builder.workersMap.toMap, builder.factoriesMap.toMap, builder.shiftsMap.toMap)
    }
  }
}

