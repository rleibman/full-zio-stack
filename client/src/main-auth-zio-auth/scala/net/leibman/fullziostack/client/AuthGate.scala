package net.leibman.fullziostack.client

import auth.{AuthClient, LoginRouter}
import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^.*
import net.leibman.fullziostack.auth.{ConnectionId, User}

/** What the client shows before letting you in: zio-auth's login screens, until someone is logged in.
  *
  * On mount it asks the server who the browser's session belongs to (`/api/whoami`, using the token zio-auth's client
  * keeps). Nobody logged in means the login screens, which also handle registering and recovering a password; once
  * that succeeds the application renders in their place.
  *
  * Those screens are zio-auth's own plain scalajs-react components rather than Material UI ones, so they don't
  * automatically follow this project's theme. `auth-*` CSS classes are what they hang their styling on.
  */
object AuthGate {

  private val connectionId = ConnectionId.random

  def apply(app: => VdomElement): VdomElement = component(() => app)

  private val component = ScalaFnComponent
    .withHooks[() => VdomElement]
    .useState(Option.empty[User])
    // None while the answer is still on its way, so the login screens don't flash in front of someone already logged in.
    .useState(true)
    .useEffectOnMountBy { (_, user, checking) =>
      AuthClient
        .whoami[User, ConnectionId](Some(connectionId))
        .map(found => user.modState(_ => found) >> checking.setState(false))
        .completeWith(_.get)
    }
    .render { (app, user, checking) =>
      if (checking.value) <.div()
      else
        user.value match {
          case Some(_) => app()
          // The login screens are a router of their own (login, register, recover), mounted in place of the app.
          case None => LoginRouter(Some(connectionId), Nil).vdomElement
        }
    }

}
