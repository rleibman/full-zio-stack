# full-zio-stack
A client/server app with a full scala 3/zio 2 stack.
These are some of the technologies I'm using, I briefly describe why and mention some options in case my choices are not yours.
One of the most difficult thing in our field is to be able to choose a set of technologies for a project that fit well together
(or can be easily made to fit), are well supported, are easy to find developers who know them (or easy to train in them), modern, etc.

## Currently on the stack

### ZIO Based

#### zio-http
#### Caliban
#### zio-config
#### zio-logging
#### zio-test
#### zio-json
#### quill
The default database layer (quill-jdbc-zio): queries are checked and turned into SQL at compile time.
#### doobie, Slick
The alternative database layers (template choices). doobie isn't strictly zio, but with the cats zio interop, it's
super easy to combine.

### Non-ZIO tech
#### scala.js (https://www.scala-js.org/)
I really wouldn't use anything else right now to create web pages. My biggest complaint about scala-js is that because all the
source documents are scala it puts graphic web designers at a disadvantage. It really forces you to separate the graphic
design domain (css) from the web content (traditionally html, now scala produced html), this is not necessarily a bad thing, but
it may force your developers to do things that normally the graphic design team takes care of.  
Alternatives: https://www.playframework.com/

#### scalajs-react (https://github.com/japgolly/scalajs-react)
I've been using scalajs-react for a long time, so historical reasons lead me to choose it. I particularly like it's use
of it's zio-like Callback (it would be even better if it actually morphed to use zio). If I was choosing today, I'd probably
give slinky a strong consideration, at least at first reading it seems a bit easier to use.
Alternatives: https://github.com/shadaj/slinky

#### scalablytyped (https://github.com/oyvindberg/ScalablyTyped)
An amazing project (that I've participated in, so I'm biased) that takes every typescript project from http://definitelytyped.org/ and
creates scala bindings for it. For React projects you can choose slinky or scalajs-react flavors. Also, coming soon, an sbt
plugin that lets you chose exactly what javascript libraries you want to wrap. 

#### Material UI (https://mui.com/material-ui/)
Replaced Semantic UI, which is no longer maintained (and needed a `findDOMNode` shim to survive React 19). The facades
come from ScalablyTyped; `client/.../components/MuiExtensions.scala` fills the few gaps in them.
Alternatives: https://react-bootstrap.github.io/

#### MariaDB / MySQL / Postgres / SQLite
MariaDB is the default. The schema lives in Flyway migrations (`db-core/src/main/resources/db/migration/<database>/`,
where MariaDB and MySQL share `mysql`), applied when the server starts. SQLite stores timestamps as integer epoch
milliseconds and supports only limited `ALTER TABLE`, which matters when you write later migrations.

## Status: becoming a template

This repository is being turned into a [Copier](https://copier.readthedocs.io/) template that generates projects on this
stack, with a choice of HTTP server, database and database layer. Until that's released, it's a runnable reference app.
`CLAUDE.md` explains how the repository is organised for that purpose.

## How the app is put together

| Module | What it is |
|---|---|
| `model/` | Domain types shared by the server and the client (cross-built for the JVM and Scala.js), and the repository contract: `Repository[F[_]]`, one `...Operations[F]` per entity, and `RepositoryError`. The server implements it with ZIO, the client with `AsyncCallback` |
| `db-core/` | `ZIORepository` (= `Repository[DataIO]`), the in-memory `MockRepository`, the pooled `DataSource` + Flyway, the migrations, and the contract test suites every database layer must pass |
| `db-quill/`, `db-doobie/`, `db-slick/` | The three database layers. Each is built once per database as `db-<layer>-<database>` (mariadb, mysql, postgres, sqlite): shared code in `src/main/scala`, the database-specific bits (Quill context, doobie mappings, Slick profile) in `src/main-<database>/scala`. The server uses `db-quill-mariadb`; every variant passes the same contract tests |
| `server-core/` | Configuration, the Caliban GraphQL API (resolved directly against the repository), and the layer wiring (`AppLayers`). Independent of the HTTP server |
| `server-ziohttp/`, `server-http4s/` | The two HTTP servers (zio-http, and http4s/ember through zio-interop-cats): GraphQL at `/api/graphql`, GraphiQL at `/api/graphiql`, `/health`, and the client's static files. Both pass the same contract tests |
| `client/` | Scala.js + scalajs-react + Material UI, bundled with vite. Talks to the server with a GraphQL client generated from the server's schema |
| `stLib/` | A standalone sbt build that generates the ScalablyTyped facades (React, MUI) |

`ModelObject` is a sample entity wired through every layer: model, migration, data service, GraphQL, and a CRUD screen.
To add your own entity, copy its pattern (every file involved has `ModelObject` in its name).

### Toolchain
Scala 3.9, sbt 2.x (the launcher picks the version from `project/build.properties`), a JDK 17+, Node/npm (for the
ScalablyTyped facades and the vite bundler), and Docker (for the development database and the database tests).

### First-time setup: the ScalablyTyped facades
The facades live in `stLib/`, a *standalone* sbt 2 build. Upstream ScalablyTyped only publishes an sbt 1 plugin, so it uses
a fork of the converter published locally from `~/projects/third-party/Converter` (see `stLib/project/plugins.sbt`). Generate
and publish them to your local ivy repository once, and again whenever `stLib/package.json` changes (then bump `version`
in `stLib/build.sbt` and `stlibVersion` in `build.sbt`):
```bash
cd stLib && npm install && sbt --error publishLocal && cd ..
```
The first run takes several minutes and several GB of memory.

### Running it
```bash
docker compose up -d                                   # MariaDB on localhost:13306, matching application.conf
sbt client/webDebugDist                                # builds the client into ./debugDist
STATIC_CONTENT_DIR=debugDist sbt server-ziohttp/run    # http://localhost:8080 (or server-http4s/run)
```
Flyway creates the tables on startup. GraphiQL is at http://localhost:8080/api/graphiql.

Configuration is in `server-core/src/main/resources/application.conf`. Every value can be overridden by the environment
variable named next to it (`HTTP_PORT`, `DB_URL`, `DB_PASSWORD`...), or the whole file with `-Dconfig.file=...`.

For a production build of the client, `sbt client/webDist` writes `./dist`, which the server serves by default.

### Changing the GraphQL API
The client's GraphQL code is generated from `server-core/src/main/graphql/schema.graphql`. After changing the API, run
`sbt server-core/calibanRender` to update that file; `SchemaSpec` fails until you do.

## Testing
```bash
sbt test       # incremental in sbt 2: reruns only the tests whose code changed
sbt testFull   # everything
```
- `CRUDOperationsContract` defines what every entity's operations must do. It runs against the in-memory mock and, through
  Testcontainers, against a real database for each database layer (Docker required).
- `ServerContractSpec` starts the HTTP server on a random port and checks it over real HTTP: health, GraphQL queries,
  mutations and error codes, GraphiQL, static files, client-side-route fallback, and path traversal.
- `SchemaSpec` checks the committed GraphQL schema matches the API.

Everything compiles with `-Werror` and `-Yexplicit-nulls`.

## Production
The server uses sbt-native-packager (`sbt server-ziohttp/Debian/packageBin` builds a Debian package with a systemd
service). Packaging choices (Docker, systemd, none) will be a template question.

## Acknowledgements
- This [blog post](https://scalac.io/making-zio-akka-slick-play-together-nicely-part-1-zio-and-slick/) uses a stack that's very similar to the one described here, I borrowed from it extensively, mostly in it's use of zio.
- [Oyvindberg](https://github.com/oyvindberg) has been super, super helpful with not only the ScalablyTyped project but with looking over my shoulder as I make mistakes.
- All the people from all the projects above that work to make the scala culture so amazing!