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
import org.json4s._
import org.json4s.Extraction._
import org.json4s.native.JsonMethods._
import sbt.IO._

case class SublimeProjectFolder(
    path: String,
    name: Option[String] = None,
    file_exclude_patterns: Option[Seq[String]] = None,
    folder_exclude_patterns: Option[Seq[String]] = None)

case class SublimeProject(
    folders: Seq[SublimeProjectFolder],
    settings: Option[JValue] = None,
    build_systems: Option[JValue] = None) {

  private implicit val formats = DefaultFormats
  def writeFile(file: File): Unit = write(file, pretty(render(decompose(this))))
}

object SublimeProject {
  private implicit val formats = DefaultFormats
  def fromFile(file: File): SublimeProject = parse(read(file)).extract[SublimeProject]
}