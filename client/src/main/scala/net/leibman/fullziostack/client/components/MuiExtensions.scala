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

package net.leibman.fullziostack.client.components

import japgolly.scalajs.react.{Callback, ReactEventFrom, ReactKeyboardEventFromInput}
import net.leibman.fullziostack.st.StBuildingComponent
import net.leibman.fullziostack.st.muiMaterial.components.Select
import net.leibman.fullziostack.st.muiMaterial.selectSelectInputMod.SelectChangeEvent
import org.scalajs.dom.{Element, HTMLInputElement, HTMLTextAreaElement}

import scala.scalajs.js

// Gaps in the ScalablyTyped MUI facades, filled in one place. Add helpers here rather than casting at call sites.

/** `Select` with the outlined variant's props. The generated `Select` offers each variant as a separate builder; this one can be started without
  * props.
  */
val OutlinedSelect: Select.OutlinedSelectPropsBaseSelectProps.type = Select.OutlinedSelectPropsBaseSelectProps

object MuiExtensions {

  /** The events of `TextField`, whose target is an `<input>`, or a `<textarea>` when `multiline`. */
  type TextFieldEvent = ReactEventFrom[(HTMLTextAreaElement | HTMLInputElement) & Element]

  /** Both elements `TextField` renders have a value. */
  extension (target: (HTMLTextAreaElement | HTMLInputElement) & Element) {

    def value: String = target.asInstanceOf[HTMLInputElement].value

  }

  /** MUI's `SelectChangeEvent<T>` is a union ScalablyTyped can't express, so the generated type lacks `target`. */
  extension [T](event: SelectChangeEvent[T]) {

    def target: js.Dynamic = event.asInstanceOf[js.Dynamic].target

  }

  extension [B <: StBuildingComponent[?]](builder: B) {

    /** `sx` from a plain style object. The generated `sx` wants `SxProps[Theme]`, a union Scala can't build literally.
      */
    def sxStyle(style: js.Object): B = builder.set("sx", style)

    /** The generated `TextField` lacks the DOM events of its root element: `BaseTextFieldProps` extends a conditional type
      * (`StandardProps<FormControlProps, ...>`), which the converter drops.
      */
    def onKeyDown(value: ReactKeyboardEventFromInput => Callback): B =
      builder.set("onKeyDown", js.Any.fromFunction1((e: ReactKeyboardEventFromInput) => value(e).runNow()))

    /** `onClose` for `Dialog` (and other modals): the converter couldn't translate `ModalProps['onClose']`, so the generated setter takes `js.Any`.
      * Runs `value` whatever the reason (backdrop click, Escape).
      */
    def onDismiss(value: Callback): B =
      builder.set(
        "onClose",
        js.Any.fromFunction2(
          (
            _: js.Any,
            _: js.Any
          ) => value.runNow()
        )
      )

  }

}
