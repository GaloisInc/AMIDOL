import sbt.Keys._
import sbt._
import sys.process._

case class WebpackFailed(proj: String) extends FeedbackProvidedException

object WebpackNpm {

  val webpack = TaskKey[Seq[File]]("webpack", "Run webpack on an npm project.")

  val projectName = settingKey[String]("Name of the project we are webpacking")
  val npmProjectDir = settingKey[File]("Folder containing the project on which webpack is run")
  val outputDir = settingKey[File]("Folder in which webpack build outputs are placed")


  // Get all regular files recursively in a folder
  def folderFilesRecursive(folder: File, filter: FileFilter): Set[File] =
    PathFinder(folder)
      .globRecursive(ExistsFileFilter && filter)
      .getPaths()
      .map(file(_))
      .toSet

  def npmWebpackTask: Def.Initialize[Task[Seq[File]]] = Def.task {
    // All the variables
    val name = projectName.value
    val streamsValue = streams.value
    val log = streamsValue.log
    val cacheDirectory = streamsValue.cacheDirectory
    val inputDirectory = npmProjectDir.value
    val outputDirectory = outputDir.value

    /* Copy all of the react sources to a cache folder somewhere in `target`.
     * Changes in this folder are what trigger webpack recompilation.
     *
     * Why can't we just track `inputDirectory` directly? Because SBT's idea of
     * detecting change is to write in two ugly new files (containing checksums or
     * timestamps) into the directory it is monitoring, and we can't have those
     * files ending up in resources... >:(
     */
    val cachedSourcesDirectory = cacheDirectory / name / "webpacking-inputs"
    IO.delete(cachedSourcesDirectory / "copied")
    IO.copyDirectory(
      source = inputDirectory,
      target = cachedSourcesDirectory / "copied",
      preserveLastModified = true,
    )

    // Run `webpack`, but only if something in the copied source directory changed
    val cachedRun = FileFunction.cached(cachedSourcesDirectory, FilesInfo.hash) { inputs =>
      log.info("Compiling with Webpack ...")

      // All the webpack stuff gets run here
      val webpackingDirectory = cacheDirectory / name / "webpacking"
      IO.copyDirectory(
        source = inputDirectory,
        target = webpackingDirectory,
      )

      // Make sure we've got all dependencies installed
      val installProcess = Process(
        command = Seq("npm", "install"),
        cwd = Some(webpackingDirectory),
      )

      // Run `webpack`, setting the output path
      val webpackProcess = Process(
        command = Seq("npm", "run", "webpack", "--", "--output-path", outputDirectory.getAbsolutePath),
        cwd = Some(webpackingDirectory),
      )

      val logger = ProcessLogger(log.info(_), log.warn(_))
      if (0 != ((installProcess #&&  webpackProcess) ! logger)) {
        log.error("Failed to process web-react")
        throw WebpackFailed(name)
      }

      /* ==Source map workaround==
       *
       * Another wrinkle: Java resources are expected to have names of the form
       * `name.extension`. That means that all the source maps, which have
       * extension `.js.map` are non-compliant. This is important because
       * `getResource` won't find a resource like `library.js.map`.
       *
       * The workaround: rename the file here and also intercept `.js.map`
       * requests in Akka HTTPs route.
       */
      for (f <- folderFilesRecursive(outputDirectory, _.name.endsWith(".js.map"))) {
        IO.move(f, file(f.absolutePath.stripSuffix("js.map") + "js-map"))
      }

      folderFilesRecursive(outputDirectory, f => Seq("js","js-map").contains(f.ext))
    }

    cachedRun(folderFilesRecursive(inputDirectory, f => Seq("js","jsx","ts","tsx").contains(f.ext))).toSeq
  }

}
