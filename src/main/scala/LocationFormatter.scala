import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class Location(
                     id: String,
                     name: String,
                     lat: Double,
                     lng: Double,
                     gmtOffset: Long,
                     createdAt: DateTime)

trait LocationFormatter extends DateTimeJsonFormat {
//  type Locations = Seq[Location]
  implicit val location: OFormat[Location] = Json.format[Location]
//  implicit val locations: OFormat[Locations] = Json.format[Locations]
}
