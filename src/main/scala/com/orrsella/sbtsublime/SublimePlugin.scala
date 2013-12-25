/**
 * Copyright (c) 2013 Orr Sella
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orrsella.sbtsublime

import sbt._

object SublimePlugin extends Plugin {
  lazy val sublimeExternalSourceDirectoryName = SettingKey[String](
    "sublime-external-source-directory-name", "The directory name for external sources")

  lazy val sublimeExternalSourceDirectoryParent = SettingKey[File](
    "sublime-external-source-directory-parent", "Parent dir of the external sources dir")

  lazy val sublimeExternalSourceDirectory = SettingKey[File](
    "sublime-external-source-directory", "Directory for external sources")

  lazy val sublimeTransitive = SettingKey[Boolean](
      "sublime-transitive",
      "Indicate whether to add sources for all dependencies transitively (including libraries that dependencies require)")

  lazy val sublimeProjectName = SettingKey[String]("sublime-project-name", "The name of the sublime project file")
  lazy val sublimeProjectDir = SettingKey[File]("sublime-project-dir", "The parent directory for the sublime project file")
  lazy val sublimeProjectFile = SettingKey[File]("sublime-project-file", "The sublime project file")

  override lazy val settings = Seq(
    Keys.commands ++= Seq(sublimeCommand, sublimeCommandCamel),
    sublimeExternalSourceDirectoryName <<= sublimeExternalSourceDirectoryName ?? "External Libraries",
    sublimeExternalSourceDirectoryParent <<= sublimeExternalSourceDirectoryParent or Keys.target,
    sublimeExternalSourceDirectory <<= sublimeExternalSourceDirectory or (sublimeExternalSourceDirectoryName, sublimeExternalSourceDirectoryParent) {
      (n, p) => new File(p, n)
    },
    sublimeTransitive <<= sublimeTransitive ?? false,
    sublimeProjectName <<= sublimeProjectName or Keys.name,
    sublimeProjectDir <<= sublimeProjectDir or Keys.baseDirectory,
    sublimeProjectFile <<= sublimeProjectFile or (sublimeProjectName, sublimeProjectDir) { (n, p) => new File(p, n + ".sublime-project") })

  lazy val sublimeCommand = Command.command("gen-sublime") { state => doCommand(state) }
  lazy val sublimeCommandCamel = Command.command("genSublime") { state => doCommand(state) }

  def doCommand(state: State): State = {
    val log = state.log
    val extracted: Extracted = Project.extract(state)
    val structure = extracted.structure
    val currentRef = extracted.currentRef
    val projectRefs = structure.allProjectRefs

    lazy val directory = (sublimeExternalSourceDirectory in currentRef get structure.data).get
    lazy val transitive = (sublimeTransitive in currentRef get structure.data).get
    lazy val projectFile = (sublimeProjectFile in currentRef get structure.data).get
    lazy val scalaVersion = (Keys.scalaVersion in currentRef get structure.data).get
    lazy val rootDirectory = (Keys.baseDirectory in currentRef get structure.data).get

    log.info("Generating Sublime project for root directory: " + rootDirectory)
    log.info("Getting dependency libraries sources transitively: " + transitive)
    log.info("Saving external sources to: " + directory)

    val dependencies: Seq[ModuleID] = projectRefs.flatMap {
      projectRef => Keys.libraryDependencies in projectRef get structure.data
    }.flatten.distinct

    val dependencyNames: Seq[String] = dependencies.map(d => d.name)

    val dependencyArtifacts: Seq[(Artifact, File)] = projectRefs.flatMap {
      projectRef => EvaluateTask(structure, Keys.updateClassifiers, state, projectRef) match {
        case Some((_, Value(report))) => report.configurations.flatMap(_.modules.flatMap(_.artifacts))
        case _ => Seq()
      }
    }.distinct

    // cleanup
    sbt.IO.delete(directory)
    sbt.IO.createDirectory(directory)

    // filter artifacts for transitive and sources only
    val filteredArtifacts =
      if (transitive) dependencyArtifacts
      // else dependencyArtifacts.filter(pair => dependencyNames.contains(pair._1.name.replace("_" + scalaVersion, "")))
      else dependencyArtifacts.filter(pair => dependencyNames.exists(name => pair._1.name.startsWith(name)))

    val sourceJars = filteredArtifacts.filter(pair => pair._1.`type` == Artifact.SourceType).map(_._2)
    log.info("Adding the following to external libraries:")
    sourceJars.foreach(jar => log.info("  " + jar.getName))

    // extract jars and make read-only
    log.info("Extracting jars to external sources directory")
    sourceJars.foreach(jar => sbt.IO.unzip(jar, new File(directory, jar.getName.replace("-sources.jar", ""))))
    log.info("Marking all files in sources directory as read-only")
    setDirectoryTreeReadOnly(directory)

    // create project file
    val srcDir = new SublimeProjectFolder(directory.getPath)
    val projectFolder = new SublimeProjectFolder(rootDirectory.getPath)
    val project =
      if (projectFile.exists) {
        val existingProject = SublimeProject.fromFile(projectFile)
        if (existingProject.folders.exists(f => f.path == directory.getPath)) existingProject
        else new SublimeProject(existingProject.folders :+ srcDir, existingProject.settings, existingProject.build_systems)
      } else new SublimeProject(Seq(projectFolder, srcDir))

    log.info("Writing project to file: " + projectFile)
    project.writeFile(projectFile)

    // return unchanged state
    state
  }

  private def setDirectoryTreeReadOnly(dir: File): Unit = {
    for (file <- dir.listFiles) {
      if (file.isDirectory) setDirectoryTreeReadOnly(file)
      else file.setReadOnly()
    }
  }
}