//> using scala 3.7.3
//> using options -deprecation -feature -Werror

// Generates template/ (the Copier template) from this repository's modules and overlay/.
//
//   scala-cli run scripts/MakeTemplate.scala            # regenerate template/
//   scala-cli run scripts/MakeTemplate.scala -- --check # fail if template/ is out of date (for CI)
//
// Every file in the repository (tracked, or untracked and not ignored) must match a rule below, even if only to be
// skipped, so a new file can't silently go missing from, or leak into, generated projects.

import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}
import java.nio.ByteBuffer
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import scala.jdk.CollectionConverters.*
import scala.sys.process.*

object MakeTemplate {

  val databases = Seq("mariadb", "mysql", "postgres", "sqlite")
  val layers = Seq("quill", "doobie", "slick")
  /** The `http_server` answer, and its module directory. */
  val servers = Seq("zio-http" -> "server-ziohttp", "http4s" -> "server-http4s")
  /** Flyway migration directories, and the databases that use each. */
  val migrationDirs = Map("mysql" -> Seq("mariadb", "mysql"), "postgres" -> Seq("postgres"), "sqlite" -> Seq("sqlite"))

  /** Where a repository file goes in the template: `target` replaces the matched `source` prefix; None skips it.
    *
    * A condition wraps one segment of the target, which then renders empty — and Copier skips a path with an empty
    * segment, along with everything under it. Put the condition on the *deepest* segment the rule owns, so
    * `template/` reads as the project it generates: `db/…/migration/{% if database == 'sqlite' %}sqlite{% endif %}/`
    * says what it means, where a conditional top-level `db` would not. Keep it above any segment that only exists in
    * this variant, though: conditioning `client/src/main/scala` alone would leave a server-only project an empty
    * `client/src/main/`.
    */
  final case class Rule(
    source: String,
    target: Option[String],
  )

  private def when(condition: String, segment: String): String = s"{% if $condition %}$segment{% endif %}"

  /** Only in projects generated with authentication (and, optionally, only when `also` holds). */
  private def authOnly(
    segment: String,
    also:    String = ""
  ): String = when(s"auth != 'none'${if (also.isEmpty) "" else s" and $also"}", segment)

  /** The test for the database(s) a migration directory serves. */
  private def databaseIs(databases: Seq[String]): String =
    if (databases.sizeIs == 1) s"database == '${databases.head}'"
    else s"database in [${databases.map(database => s"'$database'").mkString(", ")}]"

  private def skip(source: String): Rule = Rule(source, None)

  private def map(
    source: String,
    target: String,
  ): Rule = Rule(source, Some(target))

  /** First match wins, so specific rules come before general ones. */
  val rules: Seq[Rule] =
    Seq(
      // Not part of generated projects: the template machinery, and files overlay/ provides instead.
      skip("template/"),
      skip("overlay/"),
      skip("scripts/"),
      skip("tests/"),
      skip("claude-plugin/"),
      skip(".claude-plugin/"),
      skip(".github/"),
      skip("copier.yml"),
      skip("build.sbt"),
      skip("README.md"),
      skip("CLAUDE.md"),
      skip("CHANGELOG.md"),
      skip("LICENSE"),
      skip(".gitignore"),
      skip("docker-compose.yml"),
      skip("project/plugins.sbt"),
      skip("server-core/src/main/resources/application.conf"),
      skip("stLib/build.sbt"),
      // Model: the user is only part of projects with authentication.
      map(
        "model/shared/src/main/scala/net/leibman/fullziostack/auth/",
        "model/shared/src/main/scala/net/leibman/fullziostack/" + authOnly("auth") + "/",
      ),
      map("model/", "model/"),
    ) ++
      // DB: the users table and the store behind it (authentication only), which have to come before the rules that
      // take the rest of db-core...
      migrationDirs.toSeq.map { (directory, users) =>
        map(
          s"db-core/src/main/resources/db/migration/$directory/V2__app_user.sql",
          "db/src/main/resources/db/migration/" + when(databaseIs(users), directory) + "/" + authOnly("V2__app_user.sql"),
        )
      } ++
      Seq(
        map(
          "db-core/src/main/scala/net/leibman/fullziostack/auth/",
          "db/src/main/scala/net/leibman/fullziostack/" + authOnly("auth") + "/",
        ),
        map(
          "db-core/src/test/scala/net/leibman/fullziostack/auth/",
          "db/src/test/scala/net/leibman/fullziostack/" + authOnly("auth") + "/",
        ),
      ) ++
      layers.map(layer =>
        map(
          s"db-$layer/src/test/scala/net/leibman/fullziostack/auth/",
          "db/src/test/scala/net/leibman/fullziostack/" + authOnly("auth", s"db_layer == '$layer'") + "/",
        )
      ) ++
      // ...then db-core itself, plus the chosen database's migrations and test fixture...
      migrationDirs.toSeq.map { (directory, users) =>
        map(
          s"db-core/src/main/resources/db/migration/$directory/",
          "db/src/main/resources/db/migration/" + when(databaseIs(users), directory) + "/",
        )
      } ++
      databases.map(database => map(s"db-core/src/test-$database/scala/", "db/src/test/" + when(s"database == '$database'", "scala") + "/")) ++
      Seq(map("db-core/", "db/")) ++
      // ...plus the chosen layer, with its database-specific files.
      (for {
        layer    <- layers
        database <- databases
      } yield map(
        s"db-$layer/src/main-$database/scala/",
        "db/src/main/" + when(s"db_layer == '$layer' and database == '$database'", "scala") + "/",
      )) ++
      layers.map(layer => map(s"db-$layer/", when(s"db_layer == '$layer'", "db") + "/")) ++
      // Server: server-core (whose API definition and committed schema depend on the AI choice, and whose AuthModule
      // depends on the authentication one) plus the chosen HTTP server, plus the AI module when there is one.
      Seq(
        map("server-core/src/main/graphql/", "server/src/main/" + when("ai == 'none'", "graphql") + "/"),
        map("server-core/src/main-ai-none/scala/", "server/src/main/" + when("ai == 'none'", "scala") + "/"),
        map("server-core/src/main-ai-langchain4j/graphql/", "server/src/main/" + when("ai == 'langchain4j'", "graphql") + "/"),
        map("server-core/src/main-ai-langchain4j/scala/", "server/src/main/" + when("ai == 'langchain4j'", "scala") + "/"),
        map("server-core/src/main-auth-none/scala/", "server/src/main/" + when("auth == 'none'", "scala") + "/"),
        map("server-core/src/main-auth-zio-auth/scala/", "server/src/main/" + authOnly("scala") + "/"),
        map("ai-langchain4j/", when("ai == 'langchain4j'", "ai") + "/"),
        map("server-core/", "server/"),
      ) ++
      // The HTTP servers, whose zio-http one mounts the login routes. (Authentication forces http_server=zio-http,
      // so the http4s module has no auth variant to choose between.)
      Seq(
        map("server-ziohttp/src/main-auth-none/scala/", "server/src/main/" + when("http_server == 'zio-http' and auth == 'none'", "scala") + "/"),
        map("server-ziohttp/src/main-auth-zio-auth/scala/", "server/src/main/" + authOnly("scala") + "/"),
        map("server-ziohttp/src/test-auth-zio-auth/scala/", "server/src/test/" + authOnly("scala") + "/"),
      ) ++
      servers.map((server, directory) => map(s"$directory/", when(s"http_server == '$server'", "server") + "/")) ++
      Seq(
        // Client. The module itself is conditional, so these keep that condition as well as their own: without it a
        // server-only project would be left an empty client/src/main/.
        map(
          "client/src/main-auth-none/scala/",
          when("components == 'full-stack'", "client") + "/src/main/" + when("auth == 'none'", "scala") + "/",
        ),
        map(
          "client/src/main-auth-zio-auth/scala/",
          when("components == 'full-stack'", "client") + "/src/main/" + authOnly("scala") + "/",
        ),
        map("client/", when("components == 'full-stack'", "client") + "/"),
        map("stLib/", when("components == 'full-stack'", "stLib") + "/"),
        // Build
        map("project/Dependencies.scala", "project/Dependencies.scala"),
        map("project/build.properties", "project/build.properties"),
        map(".scalafmt.conf", ".scalafmt.conf"),
        map(".sbtopts", ".sbtopts"),
      )

  /** Replaced in module sources, longest first. The overlay is written in terms of the answers directly. */
  val sentinels: Seq[(String, String)] = Seq(
    "net.leibman.fullziostack" -> "{{ base_package }}",
    "net/leibman/fullziostack" -> "{{ base_package_path }}",
    "FullZIOStack"             -> "{{ class_prefix }}",
    "full-zio-stack"           -> "{{ project_slug }}",
    "Full ZIO Stack"           -> "{{ project_name }}",
  )

  /** Nothing personal may survive into generated sources. */
  val forbidden: Seq[String] = Seq("leibman", "roberto")

  /** Except where a forbidden word is part of a third-party library's own coordinates: zio-auth is published under
    * the same organization as this template, and a generated project really does depend on it by that name.
    */
  val allowed: Seq[String] = Seq("net.leibman\" % \"zio-auth", "maven.pkg.github.com/rleibman/zio-auth", "rleibman/zio-auth")

  /** sbt-header's license header. Generated projects get their own on first compile (AutomateHeaderPlugin). */
  val licenseHeader = """(?s)\A/\*\n \* Copyright \(c\) \d{4} Roberto Leibman\n.*? \*/\n\n""".r

  def main(args: Array[String]): Unit = {
    val root = Paths.get("").toAbsolutePath.nn
    require(Files.exists(root.resolve("copier.yml")), s"Run from the repository root (no copier.yml in $root)")
    val check = args.contains("--check")
    val output = if (check) Files.createTempDirectory("template-check").nn else root.resolve("template").nn

    deleteRecursively(output)
    Files.createDirectories(output)
    val problems = generate(root, output)
    if (problems.nonEmpty) {
      problems.foreach(p => System.err.println(s"ERROR: $p"))
      sys.exit(1)
    }

    if (check) {
      val differences = Process(Seq("diff", "-r", root.resolve("template").toString, output.toString)).!
      deleteRecursively(output)
      if (differences != 0) {
        System.err.println("template/ is out of date: run `scala-cli run scripts/MakeTemplate.scala`")
        sys.exit(1)
      } else println("template/ is up to date")
    } else println(s"Wrote $output")
  }

  /** Returns the problems found; writes the template into `output`. */
  def generate(
    root:   Path,
    output: Path,
  ): Seq[String] = {
    val files = Process(Seq("git", "ls-files", "--cached", "--others", "--exclude-standard"), root.toFile).!!.linesIterator
      .filter(_.nonEmpty)
      .filter(f => Files.isRegularFile(root.resolve(f)))
      .toSeq
      .sorted

    val fromModules = files.flatMap { file =>
      rules.find(rule => file == rule.source || (rule.source.endsWith("/") && file.startsWith(rule.source))) match {
        case None                   => Seq(Left(s"$file matches no rule in scripts/MakeTemplate.scala"))
        case Some(Rule(_, None))    => Seq.empty
        case Some(Rule(source, Some(target))) =>
          Seq(copyModuleFile(root.resolve(file).nn, output, target + file.stripPrefix(source)).toLeft(()))
      }
    }

    val overlay = root.resolve("overlay").nn
    val fromOverlay = Files.walk(overlay).nn.iterator.nn.asScala.filter(Files.isRegularFile(_)).map { file =>
      val target = output.resolve(overlay.relativize(file).toString).nn
      Files.createDirectories(target.getParent)
      Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
      Right(())
    }.toSeq

    (fromModules ++ fromOverlay).collect { case Left(problem) => problem }
  }

  /** Copies one module file into the template, returning a problem if there is one. */
  def copyModuleFile(
    source:     Path,
    output:     Path,
    targetPath: String,
  ): Option[String] = {
    val bytes = Files.readAllBytes(source).nn
    val renamedPath = replaceSentinels(targetPath)
    decodeText(bytes) match {
      case None =>
        write(output.resolve(renamedPath).nn, bytes)
        None
      case Some(original) =>
        val text = if (source.toString.endsWith(".scala")) licenseHeader.replaceFirstIn(original, "") else original
        val replaced = replaceSentinels(text)
        val remaining = allowed.foldLeft(replaced)((text, exception) => text.replace(exception, ""))
        val leaks = forbidden.filter(word => remaining.toLowerCase.contains(word))
        if (leaks.nonEmpty) Some(s"$source still contains ${leaks.mkString(", ")} after replacement")
        else if (replaced != text && Seq("{{", "{%", "{#").exists(original.contains))
          Some(s"$source needs rendering, but already contains Jinja delimiters")
        else {
          // Only files whose content changed are rendered by Copier; the rest are copied as they are.
          val finalPath = if (replaced != text) s"$renamedPath.jinja" else renamedPath
          write(output.resolve(finalPath).nn, replaced.getBytes(StandardCharsets.UTF_8).nn)
          None
        }
    }
  }

  /** A `{` right before a sentinel (as in `import a.{FullZIOStackApi, ...}`) would make `{{{`, which Jinja misreads, so
    * that brace is written as the expression `{{ '{' }}`.
    */
  def replaceSentinels(text: String): String =
    sentinels.foldLeft(text) { case (acc, (from, to)) => acc.replace("{" + from, "{{ '{' }}" + to).replace(from, to) }

  /** The file's text, or None if it isn't UTF-8 text. */
  def decodeText(bytes: Array[Byte]): Option[String] =
    if (bytes.contains(0.toByte)) None
    else
      try
        Some(
          StandardCharsets.UTF_8
            .newDecoder()
            .nn
            .onMalformedInput(CodingErrorAction.REPORT)
            .nn
            .decode(ByteBuffer.wrap(bytes))
            .toString
        )
      catch { case _: CharacterCodingException => None }

  def write(
    target: Path,
    bytes:  Array[Byte],
  ): Unit = {
    Files.createDirectories(target.getParent)
    Files.write(target, bytes)
  }

  def deleteRecursively(path: Path): Unit =
    if (Files.exists(path)) {
      Files.walk(path).nn.iterator.nn.asScala.toSeq.reverse.foreach(p => Files.delete(p))
    }

}
