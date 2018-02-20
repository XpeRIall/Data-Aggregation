import org.joda.time.DateTime
import play.api.libs.json.{Json, OFormat}

case class UserInfo(
                     id: String,
                     name: String,
                     email: String,
                     country: Option[String],
                     phone: Option[String],
                     birthday: String,
                     language: String,
                     createdAt: DateTime)

trait UserInfoFormatter extends DateTimeJsonFormat {
  implicit val userInfo: OFormat[UserInfo] = Json.format[UserInfo]
}
