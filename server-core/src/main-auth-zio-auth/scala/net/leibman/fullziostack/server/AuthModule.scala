package net.leibman.fullziostack.server

import auth.oauth.{OAuthService, OAuthStateStore}
import auth.{AuthConfig, AuthError, AuthServer, EmailAlreadyExists, SecretKey, UserCodePurpose}
import net.leibman.fullziostack.auth.{ConnectionId, User, UserId, UserStore}
import net.leibman.fullziostack.config.{AppConfig, AuthSettings}
import net.leibman.fullziostack.repository.RepositoryError
import zio.*

import javax.sql.DataSource

/** What authentication adds to the application: zio-auth, over the users in [[UserStore]].
  *
  * zio-auth serves the whole login workflow itself — login, logout, registration with an emailed confirmation, and
  * password recovery — as routes mounted by `AuthRoutes.secure`, and hands the rest of the server a `Session` decoded
  * from the request's bearer token. All this file does is tell it where the users are.
  *
  * The `auth` section of `application.conf` configures it; `AUTH_SECRET` signs the tokens.
  */
object AuthModule {

  /** Added to the application's environment: what mounting and serving the auth routes needs. */
  type Env = AuthConfig & AuthServer[User, UserId, ConnectionId] & OAuthService & OAuthStateStore

  /** Against the application's database, sharing its connection pool. */
  val layers: RLayer[AppConfig & DataSource, Env] = common(UserStore.live)

  /** The same, over in-memory users: for tests, or for running without a database. */
  val mockLayers: RLayer[AppConfig, Env] = common(UserStore.mock)

  // lazy, because `layers` and `mockLayers` above are built from them while this object is still initializing.
  private def common[R](userStore: RLayer[R, UserStore]): RLayer[R & AppConfig, Env] =
    ZLayer.makeSome[R & AppConfig, Env](
      userStore,
      AppConfig.auth,
      authConfig,
      authServer,
      // OAuth login (Google, Discord, Telegram) is off: configure the providers here to turn it on.
      OAuthService.live(),
      OAuthStateStore.live()
    )

  private lazy val authConfig: RLayer[AuthSettings, AuthConfig] = ZLayer.fromZIO {
    for {
      settings <- ZIO.service[AuthSettings]
      secret <- ZIO
        .succeed(settings.secret)
        .filterOrElse(_.nonEmpty)(
          // A generated secret keeps development working out of the box; every restart invalidates the sessions
          // signed with the last one, which is exactly why production must set AUTH_SECRET.
          ZIO.logWarning("app.auth.secret (AUTH_SECRET) is not set: signing sessions with a key generated for this run") *>
            Random.nextUUID.map(_.toString)
        )
    } yield AuthConfig(
      secretKey = SecretKey(secret),
      accessTTL = settings.sessionMinutes.minutes,
      refreshTTL = settings.refreshMinutes.minutes,
      secureCookie = settings.secureCookie
    )
  }

  private lazy val authServer: URLayer[AuthSettings & UserStore, AuthServer[User, UserId, ConnectionId]] =
    ZLayer.fromFunction((settings: AuthSettings, users: UserStore) =>
      new AuthServer[User, UserId, ConnectionId] {

        override def getPK(user: User): UserId = user.id

        override def login(
          email:        String,
          password:     String,
          connectionId: Option[ConnectionId]
        ): IO[AuthError, Option[User]] = users.login(email, password).mapError(asAuthError)

        override def logout(): ZIO[auth.Session[User, ConnectionId], AuthError, Unit] =
          ZIO.serviceWithZIO[auth.Session[User, ConnectionId]](session => ZIO.logInfo(s"${session.user.fold("Someone")(_.email)} logged out"))

        override def changePassword(
          userPK:      UserId,
          newPassword: String
        ): ZIO[auth.Session[User, ConnectionId], AuthError, Unit] = users.setPassword(userPK, newPassword).mapError(asAuthError)

        override def userByEmail(email: String): IO[AuthError, Option[User]] = users.byEmail(email).mapError(asAuthError)

        override def userByPK(pk: UserId): IO[AuthError, Option[User]] = users.get(pk).mapError(asAuthError)

        /** Registers the user, inactive: they become active by following the link in the confirmation email. */
        override def createUser(
          name:     String,
          email:    String,
          password: String
        ): IO[AuthError, User] = users.create(name, email, password).mapError(asAuthError)

        override def activateUser(userPK: UserId): IO[AuthError, Unit] = users.activate(userPK).mapError(asAuthError)

        /** Logs the email rather than sending it: a template can't know your mail provider. Development works as it
          * is — the confirmation and password-recovery links are in the server's log, ready to be pasted into a
          * browser — but before you let strangers register, send them for real from here (zio-auth's own dependencies
          * include courier, an SMTP client).
          */
        override def sendEmail(
          subject: String,
          body:    String,
          user:    User
        ): IO[AuthError, Unit] =
          ZIO.logInfo(s"Email not sent (no mail provider configured). To: ${user.email}, subject: $subject\n$body")

        override def getEmailBodyHtml(
          user:    User,
          purpose: UserCodePurpose,
          url:     String
        ): String = {
          val link = s"${settings.webHostUrl}/#$url"
          val (heading, instruction) = purpose match {
            case UserCodePurpose.NewUser      => ("Welcome!", "Confirm your registration")
            case UserCodePurpose.LostPassword => ("Forgotten password", "Choose a new password")
          }
          s"""<html><body>
             |  <h1>$heading</h1>
             |  <p>Hello ${user.name},</p>
             |  <p>$instruction by following this link: <a href="$link">$link</a></p>
             |  <p>The link expires in a couple of days.</p>
             |</body></html>""".stripMargin
        }

      }
    )

  /** zio-auth has one error type; a duplicate email is the one failure it treats specially. */
  private def asAuthError(error: RepositoryError): AuthError =
    error match {
      case conflict: RepositoryError.Conflict => EmailAlreadyExists(conflict.getMessage)
      case other => AuthError(other)
    }

}
