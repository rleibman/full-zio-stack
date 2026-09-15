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

package net.leibman.fullziostack.client.pages

import japgolly.scalajs.react.*
import japgolly.scalajs.react.vdom.html_<^.*
import net.leibman.fullziostack.client.components.MuiExtensions.*
import net.leibman.fullziostack.client.components.OutlinedSelect
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.st.muiMaterial.components.{Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, MenuItem, TextField}
import net.leibman.fullziostack.st.muiMaterial.muiMaterialStrings as MuiStrings
import net.leibman.fullziostack.st.muiSystem.muiSystemStrings as SystemStrings

import scala.scalajs.js

/** Create/edit dialog for a ModelObject. Saving is left to the caller, which knows what to reload afterwards. */
object ModelObjectForm {

  case class Props(
    initial:  ModelObject,
    saving:   Boolean,
    onSave:   ModelObject => Callback,
    onCancel: Callback
  )

  private val component = ScalaFnComponent[Props] { props =>
    for {
      form <- useState(props.initial)
    } yield {
      val isNew = props.initial.id == ModelObjectId.empty
      val valid = !form.value.name.isBlank
      Dialog(open = true)
        .fullWidth(true)
        .maxWidth(SystemStrings.sm)
        .onDismiss(props.onCancel)(
          DialogTitle()(if (isNew) "New model object" else s"Edit ${props.initial.name}"),
          DialogContent()(
            Box().sxStyle(js.Dynamic.literal(display = "flex", flexDirection = "column", gap = 2, pt = 1))(
              TextField()
                .label("Name")
                .required(true)
                .fullWidth(true)
                .autoFocus(true)
                .value(form.value.name)
                .error(!valid)
                .helperText(if (valid) "" else "A name is required")
                .onChange((e: TextFieldEvent) => form.modState(_.copy(name = e.target.value))),
              TextField()
                .label("Description")
                .fullWidth(true)
                .multiline(true)
                .minRows(3)
                .value(form.value.description)
                .onChange((e: TextFieldEvent) => form.modState(_.copy(description = e.target.value))),
              OutlinedSelect
                .label("Type")
                .fullWidth(true)
                .value(form.value.modelObjectType.toString)
                .onChange(
                  (
                    e,
                    _
                  ) => form.modState(_.copy(modelObjectType = ModelObjectType.valueOf(e.target.value.asInstanceOf[String])))
                )(
                  ModelObjectType.values.toSeq.map(t => MenuItem.value(t.toString).withKey(t.toString)(t.toString).build)*
                )
            )
          ),
          DialogActions()(
            Button.onClick(_ => props.onCancel)("Cancel"),
            Button
              .variant(MuiStrings.contained)
              .disabled(!valid || props.saving)
              .onClick(_ => props.onSave(form.value))(if (props.saving) "Saving..." else "Save")
          )
        )
    }
  }

  def apply(
    initial:  ModelObject,
    saving:   Boolean,
    onSave:   ModelObject => Callback,
    onCancel: Callback
  ): VdomElement = component(Props(initial, saving, onSave, onCancel))

}
