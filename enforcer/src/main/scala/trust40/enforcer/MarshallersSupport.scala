package trust40.enforcer

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import spray.json.DefaultJsonProtocol

trait MarshallersSupport extends SprayJsonSupport with DefaultJsonProtocol {

  final case class Person(id: Int, firstName: String, lastName: String, salary: Double)

  implicit val personFormat = jsonFormat4(Person) // The number in jsonFormatXX corresponds to the number of fields of the case class


  implicit val intMarshaller: ToEntityMarshaller[Int] =
    Marshaller.opaque((v: Int) => HttpEntity(ContentTypes.`text/plain(UTF-8)`, v.toString))

  implicit val intUnmarshaller: FromEntityUnmarshaller[Int] =
    Unmarshaller.stringUnmarshaller.map(str => str.toInt)
}