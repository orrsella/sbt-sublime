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

import java.io.File
import sbt._
import sbt.IO._
import sbt.Keys._

object SbtSublimePlugin extends Plugin {
  lazy val sublime = TaskKey[Unit]("gen-sublime", "Generate Sublime Text 2 project")
  lazy val sublimeLibraryDependenciesDirectoryName = SettingKey[String]("sublime-ext-src-dir-name",
    "The directory name for external sources")
  lazy val sublimeLibraryDependenciesDirectoryParent = SettingKey[File]("sublime-ext-src-dir-parent",
    "Parent dir of the external sources dir")
  lazy val sublimeLibraryDependenciesDirectory = SettingKey[File]("sublime-ext-src-dir", "Directory for external sources")
  lazy val sublimeLibraryDependenciesTransitive = SettingKey[Boolean]("sublime-ext-src-transitive",
    "Indicate whether to add sources for all dependencies transitively (including the libraries that your own " +
    "dependencies require)")
  lazy val sublimeProjectName = SettingKey[String]("sublime-project-name", "The name of the sublime project file")
  lazy val sublimeProjectDir = SettingKey[File]("sublime-project-dir", "The parent directory for the sublime project file")
  lazy val sublimeProjectFile = SettingKey[File]("sublime-project-file", "The sublime project file")

  lazy val sublimeTaskSetting = sublime <<=
    (baseDirectory,
    target,
    updateClassifiers,
    libraryDependencies,
    scalaVersion,
    sublimeLibraryDependenciesDirectory,
    sublimeLibraryDependenciesTransitive,
    sublimeProjectFile) map { (base, tar, rep, dep, ver, dir, tran, proj) => gen(base, tar, rep, dep, ver, dir, tran, proj) }

  override lazy val projectSettings = super.projectSettings ++ Seq(
    sublimeTaskSetting,
    sublimeLibraryDependenciesDirectoryName := "External Libraries",
    sublimeLibraryDependenciesDirectoryParent <<= target,
    sublimeLibraryDependenciesDirectory <<=
      (sublimeLibraryDependenciesDirectoryName, sublimeLibraryDependenciesDirectoryParent) { (n, p) => new File(p, n) },
    sublimeLibraryDependenciesTransitive := true,
    sublimeProjectName <<= (name) { (name) => name},
    sublimeProjectDir <<= baseDirectory,
    sublimeProjectFile <<= (sublimeProjectName, sublimeProjectDir) { (n, p) => new File(p, n + ".sublime-project") },
    cleanFiles <+= (sublimeLibraryDependenciesDirectory) { dir => dir })

  private def gen(
    baseDirectory: File,
    target: File,
    updateReport: UpdateReport,
    dependencies: Seq[ModuleID],
    scalaVersion: String,
    directory: File,
    transitive: Boolean,
    projectFile: File) = {

    // cleanup
    delete(directory)
    createDirectory(directory)

    // calc jars list
    val dependenciesNames = dependencies.map(d => d.organization + ":" + d.name)
    val sourceJars: Seq[File] = updateReport.configurations.flatMap {
      if (transitive)
        c => c.modules.flatMap(_.artifacts).filter(_._1.`type` == Artifact.SourceType).map(_._2)
      else
        c => c.modules.filter(
          m => dependenciesNames.contains(
            m.module.organization + ":" + m.module.name.replace("_" + scalaVersion, ""))
          ).flatMap(_.artifacts).filter(_._1.`type` == Artifact.SourceType).map(_._2)
    }.distinct

    // extract jars and make read-only
    sourceJars.foreach(j => unzip(j, new File(directory, j.getName.replace("-sources.jar", ""))))
    setDirectoryTreeReadOnly(directory)

    // create project file
    val libFolder = new SublimeProjectFolder(directory.getPath)
    val projectFolder = new SublimeProjectFolder(baseDirectory.getPath)
    val project =
      if (projectFile.exists) {
        val existingProject = SublimeProject.fromFile(projectFile)
        if (existingProject.folders.exists(f => f.path == directory.getPath)) existingProject
        else new SublimeProject(existingProject.folders :+ libFolder, existingProject.settings, existingProject.build_systems)
      } else new SublimeProject(Seq(projectFolder, libFolder))

    project.toFile(projectFile)
  }

  private def setDirectoryTreeReadOnly(dir: File): Unit = {
    for (file <- dir.listFiles) {
      if (file.isDirectory) setDirectoryTreeReadOnly(file)
      else file.setReadOnly
    }
  }
}