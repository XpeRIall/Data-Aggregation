import play.api.libs.json.{Json, OFormat}


case class User(email: String, whenIssued: String, whenExpires: String, policies: Array[String], jwtToken: String)

trait UserFormatter {
  implicit val user: OFormat[User] = Json.format[User]
}
