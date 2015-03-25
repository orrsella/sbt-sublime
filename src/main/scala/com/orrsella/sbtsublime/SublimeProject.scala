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
import sbt.IO._
import spray.json.DefaultJsonProtocol._
import spray.json._

case class SublimeProject(
    folders: Seq[SublimeProjectFolder],
    settings: Option[JsValue] = None,
    build_systems: Option[JsValue] = None)

object SublimeProject {
  private implicit val folderFormat: JsonFormat[SublimeProjectFolder] = jsonFormat4(SublimeProjectFolder.apply)
  private implicit val projectFormat: JsonFormat[SublimeProject] = jsonFormat3(SublimeProject.apply)

  def fromFile(file: File): SublimeProject = read(file).parseJson.convertTo[SublimeProject]
  def writeFile(file: File, project: SublimeProject): Unit = write(file, project.toJson.prettyPrint)
}
