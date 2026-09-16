package net.leibman.fullziostack.server

import auth.*
import net.leibman.fullziostack.auth.{ConnectionId, User, UserId}
import zio.*
import zio.http.*

/** Mounts zio-auth's routes and puts the application's own behind a session.
  *
  * zio-auth splits its routes in two: `unauthRoutes` are the ones you reach without being logged in (login,
  * registration, password recovery, and the client's copy of the auth configuration), and `authRoutes` are the ones
  * that need a session (whoami, logout). `bearerSessionProvider` is what decodes the request's bearer token into that
  * `Session`, so everything it wraps can ask for one.
  *
  * The application's routes are wrapped with it too: they don't require a session today, which is why an anonymous
  * request still reaches the GraphQL API. To make one of them refuse anonymous callers, ask for the session and match
  * on it -- `ZIO.serviceWithZIO[Session[User, ConnectionId]]`, answering `Response.unauthorized` for anything but an
  * `AuthenticatedSession`.
  */
object AuthRoutes {

  def secure[R](routes: Routes[R, Response]): URIO[AuthModule.Env, Routes[R & AuthModule.Env, Response]] =
    for {
      server       <- ZIO.service[AuthServer[User, UserId, ConnectionId]]
      authRoutes   <- server.authRoutes
      unauthRoutes <- server.unauthRoutes
    } yield ((routes ++ asResponses(authRoutes)) @@ server.bearerSessionProvider) ++ asResponses(unauthRoutes)

  /** zio-auth's routes fail with an AuthError; the rest of the server has already turned its failures into responses. */
  private def asResponses[R](routes: Routes[R, AuthError]): Routes[R, Nothing] =
    routes.handleErrorCause { cause =>
      cause.squash match {
        case ExpiredToken(message, _)   => Response.error(Status.Unauthorized, message)
        case InvalidToken(message, _)   => Response.error(Status.Unauthorized, message)
        case NotAuthenticated           => Response.unauthorized
        case AuthBadRequest(message, _) => Response.error(Status.BadRequest, message)
        case EmailAlreadyExists(email)  => Response.error(Status.Conflict, email)
        // Anything else is a bug or a broken dependency: say so without leaking its details to the caller.
        case _ => Response.internalServerError
      }
    }

}
