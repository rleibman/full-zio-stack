import { defineConfig } from "vite";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));

// This config is driven by sbt (see `client/webDist` / `client/webDebugDist` in build.sbt), NOT the other way around.
//
// The usual community setup is @scala-js/vite-plugin-scalajs, which resolves `scalajs:main.js` by spawning
// `sbt print fastLinkJSOutput` as a child process. That inverts the dependency -- npm drives sbt -- and running
// it from inside our own dist task would mean sbt re-entering sbt, fighting over the server lock, for a path the
// calling task already knows. So sbt hands the two paths vite needs over in the environment.
function required(name) {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is not set; run this build through sbt's client/webDist or client/webDebugDist.`);
  return value;
}

const scalaJSOutputDir = required("SCALAJS_OUTPUT_DIR");
const outDir = required("VITE_OUT_DIR");

// The Scala.js linker output imports npm packages by bare name (react, @mui/material, ...). Node resolution
// walks UP from the importing file to find node_modules -- and under sbt 2 that output lives at
// <repo>/target/out/sjs1/..., nowhere near client/node_modules, so every bare import fails to resolve.
// Re-resolve those imports as if they came from client/, where node_modules is.
const resolveScalaJSImportsFromClient = {
  name: "scalajs-bare-imports",
  enforce: "pre",
  async resolveId(source, importer, options) {
    if (!importer || !importer.startsWith(scalaJSOutputDir)) return null;
    if (source.startsWith(".") || path.isAbsolute(source)) return null;
    const resolved = await this.resolve(source, path.join(here, "main.js"), { ...options, skipSelf: true });
    return resolved ?? null;
  },
};

export default defineConfig(({ mode }) => ({
  root: here,
  // Static assets (client/src/main/web) are copied by viteDistImpl in build.sbt.
  publicDir: false,
  build: {
    outDir,
    // outDir is a staging directory under target/ that only this build writes, so emptying it is safe and keeps
    // hashed bundles from piling up. dist/ and debugDist/ are a separate copy step in build.sbt.
    emptyOutDir: true,
    minify: mode === "production",
    // Default is 500 kB, which a Scala.js app trips unconditionally.
    chunkSizeWarningLimit: 3500,
    rolldownOptions: {
      // Dependencies carry "use client" directives the bundler cannot honour, and ship sourcemaps pointing at
      // paths that do not exist. Both are noise from code we do not control -- drop them for node_modules only,
      // so the same warnings from OUR code still surface.
      onwarn(warning, warn) {
        const fromDependency = (warning.id ?? warning.loc?.file ?? "").includes("node_modules");
        if (fromDependency && warning.code === "MODULE_LEVEL_DIRECTIVE") return;
        if (fromDependency && warning.code === "SOURCEMAP_ERROR") return;
        warn(warning);
      },
      output: {
        // Split npm dependencies out of the app bundle, so a Scala change does not invalidate React and MUI
        // along with it. Vendor code changes only when package.json does.
        codeSplitting: {
          groups: [{ name: "vendor", test: /node_modules/ }],
        },
      },
    },
    // Production stack traces are unreadable without this.
    sourcemap: true,
  },
  resolve: {
    alias: [{ find: /^scalajs$/, replacement: path.resolve(scalaJSOutputDir, "main.js") }],
  },
  plugins: [resolveScalaJSImportsFromClient],
}));
