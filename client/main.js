// Vite's entry point. `scalajs` is aliased in vite.config.js to the linker output directory sbt hands over;
// importing it runs the app's main (Compile / scalaJSUseMainModuleInitializer := true).
// Roboto is MUI's default typeface; bundling it keeps the app free of CDN requests.
import "@fontsource/roboto/300.css";
import "@fontsource/roboto/400.css";
import "@fontsource/roboto/500.css";
import "@fontsource/roboto/700.css";
import "scalajs";
