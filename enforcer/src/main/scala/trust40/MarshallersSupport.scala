package trust40

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import trust40.k4case.{Position, SimulationState, WorkerState}

trait MarshallersSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val positionFormat = jsonFormat2(Position)
  implicit val workerStateFormat = jsonFormat1(WorkerState)
  implicit val simulationStateFormat = jsonFormat3(SimulationState)
}
