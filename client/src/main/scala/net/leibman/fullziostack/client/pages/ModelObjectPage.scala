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
import net.leibman.fullziostack.client.api.ModelObjectApi
import net.leibman.fullziostack.client.components.MuiExtensions.*
import net.leibman.fullziostack.client.components.OutlinedSelect
import net.leibman.fullziostack.model.*
import net.leibman.fullziostack.st.muiMaterial.components.{
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  LinearProgress,
  MenuItem,
  Paper,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography
}
import net.leibman.fullziostack.st.muiMaterial.muiMaterialStrings as MuiStrings

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import scala.scalajs.js

/** Search, page through, create, edit and delete ModelObjects. The pattern to copy for a new entity's page. */
object ModelObjectPage {

  /** Everything that decides what's on screen. `revision` forces a reload after a save or delete. */
  private case class Query(
    text:            String = "",
    modelObjectType: Option[ModelObjectType] = None,
    includeDeleted:  Boolean = false,
    page:            Int = 0,
    rowsPerPage:     Int = 10,
    revision:        Int = 0
  ) {

    def search: ModelObjectSearch =
      ModelObjectSearch(
        text = Option(text.trim).filter(_.nonEmpty),
        modelObjectType = modelObjectType,
        includeDeleted = includeDeleted,
        offset = page * rowsPerPage,
        limit = rowsPerPage
      )

  }

  private given Reusability[Query] = Reusability.by_==

  private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").nn.withZone(ZoneId.systemDefault()).nn

  private val anyType = "any"

  private val component = ScalaFnComponent[Unit] { _ =>
    for {
      query    <- useState(Query())
      results  <- useState(Page(Seq.empty[ModelObject], 0L))
      loading  <- useState(false)
      saving   <- useState(false)
      error    <- useState(Option.empty[String])
      editing  <- useState(Option.empty[ModelObject])
      deleting <- useState(Option.empty[ModelObject])
      _        <- useEffectWithDeps(query.value) { q =>
        (loading.setState(true).asAsyncCallback >>
          ModelObjectApi.search(q.search).flatMap(page => results.setState(page).asAsyncCallback))
          .handleError(e => error.setState(Some(e.getMessage)).asAsyncCallback)
          .finallyRun(loading.setState(false).asAsyncCallback)
          .toCallback
      }
    } yield {
      val reload: Callback = query.modState(q => q.copy(revision = q.revision + 1))

      def save(obj: ModelObject): Callback =
        (saving.setState(true).asAsyncCallback >> ModelObjectApi.upsert(obj) >> (editing.setState(None) >> reload).asAsyncCallback)
          .handleError(e => error.setState(Some(e.getMessage)).asAsyncCallback)
          .finallyRun(saving.setState(false).asAsyncCallback)
          .toCallback

      def delete(obj: ModelObject): Callback =
        (ModelObjectApi.delete(obj.id, softDelete = true) >> (deleting.setState(None) >> reload).asAsyncCallback)
          .handleError(e => error.setState(Some(e.getMessage)).asAsyncCallback)
          .toCallback

      <.div(
        Box().sxStyle(js.Dynamic.literal(display = "flex", gap = 2, alignItems = "center", flexWrap = "wrap", mb = 2))(
          TextField()
            .label("Search")
            .size(MuiStrings.small)
            .value(query.value.text)
            .onChange((e: TextFieldEvent) => query.modState(_.copy(text = e.target.value, page = 0))),
          OutlinedSelect
            .size(MuiStrings.small)
            .value(query.value.modelObjectType.fold(anyType)(_.toString))
            .onChange {
              (
                e,
                _
              ) =>
                val selected = e.target.value.asInstanceOf[String]
                query.modState(_.copy(modelObjectType = ModelObjectType.values.find(_.toString == selected), page = 0))
            }(
              (MenuItem.value(anyType).withKey(anyType)("Any type").build +:
                ModelObjectType.values.toSeq.map(t => MenuItem.value(t.toString).withKey(t.toString)(t.toString).build))*
            ),
          FormControlLabel(
            Checkbox
              .checked(query.value.includeDeleted)
              .onChange(
                (
                  _,
                  checked
                ) => query.modState(_.copy(includeDeleted = checked, page = 0))
              )
              .build
          ).label("Include deleted"),
          Box().sxStyle(js.Dynamic.literal(flexGrow = 1))(),
          Button
            .variant(MuiStrings.contained)
            .onClick(_ => editing.setState(Some(ModelObject(name = ""))))("New")
        ),
        Paper()(
          if (loading.value) LinearProgress() else Box().sxStyle(js.Dynamic.literal(height = 4))(),
          TableContainer()(
            Table().size(MuiStrings.small)(
              TableHead()(
                TableRow()(
                  TableCell()("Name"),
                  TableCell()("Description"),
                  TableCell()("Type"),
                  TableCell()("Last updated"),
                  TableCell().align(MuiStrings.right)("")
                )
              ),
              TableBody()(
                if (results.value.items.isEmpty && !loading.value)
                  TableRow()(TableCell().colSpan(5)(Typography().color("text.secondary")("Nothing here yet.")))
                else
                  results.value.items.map { obj =>
                    TableRow()
                      .withKey(obj.id.value.toString)(
                        TableCell()(obj.name, if (obj.deleted) " (deleted)" else ""),
                        TableCell()(obj.description),
                        TableCell()(obj.modelObjectType.toString),
                        TableCell()(timestampFormat.format(obj.lastUpdated)),
                        TableCell().align(MuiStrings.right)(
                          Button.size(MuiStrings.small).onClick(_ => editing.setState(Some(obj)))("Edit"),
                          Button
                            .size(MuiStrings.small)
                            .color(MuiStrings.error)
                            .disabled(obj.deleted)
                            .onClick(_ => deleting.setState(Some(obj)))("Delete")
                        )
                      ).build
                  }.toVdomArray
              )
            )
          ),
          TablePagination(
            count = results.value.total.toDouble,
            onPageChange = (
              _,
              page
            ) => query.modState(_.copy(page = page.toInt)),
            page = query.value.page.toDouble,
            rowsPerPage = query.value.rowsPerPage.toDouble
          ).component("div")
            .rowsPerPageOptionsVarargs(5.0, 10.0, 25.0, 50.0)
            .onRowsPerPageChange(e => query.modState(_.copy(rowsPerPage = e.target.value.toInt, page = 0)))
        ),
        editing.value.map(obj => ModelObjectForm(initial = obj, saving = saving.value, onSave = save, onCancel = editing.setState(None))),
        deleting.value.map(obj =>
          Dialog(open = true)
            .onDismiss(deleting.setState(None))(
              DialogTitle()(s"Delete ${obj.name}?"),
              DialogContent()(DialogContentText()("It will be marked as deleted, and hidden unless you include deleted objects.")),
              DialogActions()(
                Button.onClick(_ => deleting.setState(None))("Cancel"),
                Button.color(MuiStrings.error).variant(MuiStrings.contained).onClick(_ => delete(obj))("Delete")
              )
            ).build
        ),
        Snackbar
          .open(error.value.isDefined)
          .autoHideDuration(6000)
          .onClose(
            (
              _,
              _
            ) => error.setState(None)
          )(
            Alert.severity(MuiStrings.error).onClose(_ => error.setState(None))(error.value.getOrElse(""))
          )
      )
    }
  }

  def apply(): VdomElement = component()

}
