// COMPAT SHIM, same as chuti's and meal-o-rama's web/react-dom-compat.js.
//
// React 19 removed ReactDOM.findDOMNode from the public API. Callers that still reach for it throw,
// React unmounts the whole tree, and the app renders a blank page.
//
// The caller here is semantic-ui-react's <Ref>, via @fluentui/react-component-ref (RefFindNode.js).
// semantic-ui-react 2.1.5 is the latest release and still declares react <= 18 as its peer, so this stays
// until semantic-ui-react is replaced (or 3.x leaves beta). We are on React 19 because scalajs-react 4.x
// is written for it.
//
// React 19 only removed the *export*: react-dom/client still installs the real implementation on
// react-dom's internals object. So re-export everything react-dom exports, plus findDOMNode, and have
// vite alias "react-dom" to this file (see resolve.alias in vite.config.js).
//
// The import below uses a relative path so it bypasses the alias and reaches the real package.
// Do NOT import react-dom/client here: it imports "react-dom", which the alias points back at this
// file, and the resulting cycle leaves these exports uninitialised. The app loads react-dom/client
// itself (createRoot), which is what installs findDOMNode on the internals -- so look it up lazily,
// at call time, by which point it is always present.
import ReactDOM from "./node_modules/react-dom/index.js";

export function findDOMNode(componentOrElement) {
  const internals = ReactDOM.__DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE;
  if (typeof internals?.findDOMNode !== "function") {
    throw new Error("react-dom-compat: findDOMNode is unavailable in this react-dom build");
  }
  return internals.findDOMNode(componentOrElement);
}

export const {
  __DOM_INTERNALS_DO_NOT_USE_OR_WARN_USERS_THEY_CANNOT_UPGRADE,
  createPortal,
  flushSync,
  preconnect,
  prefetchDNS,
  preinit,
  preinitModule,
  preload,
  preloadModule,
  requestFormReset,
  unstable_batchedUpdates,
  useFormState,
  useFormStatus,
  version,
} = ReactDOM;

export default { ...ReactDOM, findDOMNode };
