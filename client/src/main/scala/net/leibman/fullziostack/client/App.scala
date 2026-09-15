/*
 * Copyright (c) 2024 Roberto Leibman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.leibman.fullziostack.client

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import net.leibman.fullziostack.client.api.ModelObjectApi
import net.leibman.fullziostack.client.components.MuiExtensions.*
import net.leibman.fullziostack.client.pages.ModelObjectPage
import net.leibman.fullziostack.st.muiMaterial.components.{AppBar, Box, Container, CssBaseline, ThemeProvider, Toolbar, Typography}
import net.leibman.fullziostack.st.muiMaterial.muiMaterialStrings as MuiStrings
import net.leibman.fullziostack.st.muiSystem.muiSystemStrings as SystemStrings

import scala.scalajs.js

/** The application shell: theme, top bar, and the page. Add navigation here when there's more than one page. */
object App {

  private val component = ScalaFnComponent[Unit] { _ =>
    for {
      version <- useState("")
      _ <- useEffectOnMount(ModelObjectApi.version.flatMap(v => version.setState(v).asAsyncCallback).handleError(_ => AsyncCallback.unit).toCallback)
    } yield ThemeProvider(Theme.theme)(
      CssBaseline(),
      AppBar.position(MuiStrings.static)(
        Toolbar()(
          Typography.variant(MuiStrings.h6).sxStyle(js.Dynamic.literal(flexGrow = 1))("Full ZIO Stack"),
          Typography.variant(MuiStrings.caption)(version.value)
        )
      ),
      Container.maxWidth(SystemStrings.lg)(
        Box().sxStyle(js.Dynamic.literal(py = 3))(ModelObjectPage())
      )
    )
  }

  def apply(): VdomElement = component()

}
