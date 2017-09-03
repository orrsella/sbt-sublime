# sbt-sublime

An [sbt](http://www.scala-sbt.org/) (Simple Build Tool) plugin for generating [Sublime Text](http://www.sublimetext.com/) 2 or 3 project files with library dependencies' sources. See [example screenshot](https://github.com/orrsella/sbt-sublime#example).

The main goal of this plugin is to make dependency source files easily available in the project's Sublime window. This enables the ability to simultaneously code and browse all your dependencies' source code, similar to functionality that IntelliJ and other IDEs provide you with. This also means that external library source code plays nice with Sublime's excellent Goto Anything feature. It could also be useful for split editing. Don't remember a method's signature in `scala.collection.immutable.List`? Just hit CMD+T, enter "List" and the source is right in front of you (the scala-library is always a dependency so you have the entire Scala language code base a click away).

See [Functionality](https://github.com/orrsella/sbt-sublime#functionality) and [Notes](https://github.com/orrsella/sbt-sublime#notes) for more details, and [this post](http://orrsella.com/2013/01/28/scala-development-using-sublime-text-2/) for some background.

## Add Plugin

To add sbt-sublime functionality to your project add the following to your `project/plugins.sbt` file:

```scala
// sbt 1.0:
addSbtPlugin("com.orrsella" % "sbt-sublime" % "1.1.2")

// sbt 0.13:
addSbtPlugin("com.orrsella" % "sbt-sublime" % "1.1.1")
```

If you want to use it for more than one project, you can add it to your global plugins file, usually found at: `~/.sbt/0.13/plugins/plugins.sbt` and then have it available for all sbt projects. See [Using Plugins](http://www.scala-sbt.org/release/docs/Getting-Started/Using-Plugins.html) for additional information on sbt plugins.

If you'd like to configure the global plugin you'll need to add an sbt file in `~/.sbt/0.13/`. In that file you need to import `com.orrsella.sbtsublime.SublimePlugin.autoImport._` and then you can [configure](https://github.com/orrsella/sbt-sublime#configuration) as normal.

### Requirements

* sbt 1.0+, Scala 2.12.x
* sbt 0.13.5+ (requires `AutoPlugin`), Scala 2.10.x

### Troubleshooting

If you added the plugin globally but still don't have the `gen-sublime` command available, try:

```
$ sbt
> reload plugins
> clean
> reload return
```

Essentially, this enters the `project` project, cleans it, and returns back to your main project (remember that [sbt is recursive](http://www.scala-sbt.org/release/docs/Getting-Started/Full-Def.html#sbt-is-recursive) – each `project/` folder is an sbt project in itself!).

## Example

As an example, here's the project file generated for [Twitter's bijection](https://github.com/twitter/bijection) project. The first folder, `bijection`, is the project's root as cloned from GitHub. The `External Libraries` folder is the generated external sources folder, showing all available dependencies' sources, readily available for you to browse or search (BTW, in case you're wondering, the theme I'm using is  the immensely popular Soda Light):

![alt text](https://raw.github.com/orrsella/sbt-sublime/master/img/screenshot1.png "Generated project for Twitter's bijection")

## Usage

To use sbt-sublime, simply enter the `gen-sublime` command in the sbt console to create the project file. When the command is done, open the new Sublime project created to see your own sources and external library sources.

## Functionality

* Creates a `.sublime-project` project file for your project. The default project file created will include the project's base directory and the special external library sources directory. If a project file already exists, the plugin will keep all existing settings in the file and only add the external sources directory. You don't have to worry about losing your Sublime project's settings.

* Automatically fetches sources available for all dependencies.

* Allows fetching all dependencies transitively – have access to the sources of all libraries that your own dependencies require. This can quickly escalate to *a lot* of source code, so the default behavior is to not fetch dependencies transitively, only your direct dependencies (see [next section](https://github.com/orrsella/sbt-sublime#configuration)).

* Works with multi-project build configurations. In this scenario, external libraries will include the dependencies of all projects combined. **Important:** make sure to run the `gen-sublime` command on the root project. Otherwise, you'll create a Sublime project for the sub-project you ran the command on. Not the end of the world, but probably not what you meant to happen.

## Configuration

The following custom sbt settings are used:

* `sublimeExternalSourceDirectoryName` – The name of the directory containing all external library source files. Default value: `External Libraries`.

* `sublimeExternalSourceDirectoryParent` – Where the external library sources directory will be located. Default value: sbt's `target` setting. If left unchanged, running the `clean` command will delete the sources folder. To have it persist, change it's parent away from the target folder.

* `sublimeTransitive` – Indicates whether dependencies should be added transitively (recursively) for all libraries (including the libraries that your own dependencies require – "your dependencies' dependencies"). For large projects, this can amount to dozens of libraries pretty quickly, meaning that *a lot* of code will be searched and handled by Sublime. See if appropriate for your own project. Default value: `false`.

* `sublimeProjectName` – The name of the generated Sublime project file, not including the ".sublime-project" extension. Default value: sbt's `name` setting, that is your project's name as you define it in `build.sbt`.

* `sublimeProjectDir` – Where the generated Sublime project file will be saved. Default value: sbt's `baseDirectory` setting, that is the root of your project. This can be set to anywhere on your machine, it doesn't have to be in the project's root directory (but would be convenient). If you already have a project file, or like to keep all project files together in some special folder, just point there.

* `sublimeFileExcludePatterns` – Optional file patterns to be excluded from the project tree. (See [Projects](http://www.sublimetext.com/docs/3/projects.html) for more details.)

* `sublimeFolderExcludePatterns` – Optional folder patterns to be excluded from the project tree. (See [Projects](http://www.sublimetext.com/docs/3/projects.html) for more details.)

To change any/all of these settings (to these arbitrary alternative values), add the following to your `build.sbt` file:

```scala
sublimeExternalSourceDirectoryName := "ext-lib-src"

sublimeExternalSourceDirectoryParent <<= crossTarget

sublimeTransitive := true

sublimeProjectName := "MySublProjectFile"

sublimeProjectDir := new java.io.File("/Users/orr/Dev/Projects")

sublimeFileExcludePatterns := Seq("*.css")

sublimeFolderExcludePatterns := Seq("backup", "target")
```

## Notes

* The external library sources directory is considered as artifacts and located by default in `target`, and so running the `clean` command will delete it. But don't worry – you can always re-run `gen-sublime` to get it back, or change `sublimeExternalSourceDirectoryParent` to have it reside out side of the `target` folder and not get deleted during `clean`.

* When running the `gen-sublime` command the existing library sources directory is deleted, and a new one is created.

* All library source files are intentionally marked as read-only so you won't be able to save changes to them. This is mainly to remind you that changing these sources has *absolutely no* effect on the libraries you're using! **This is important** – just because the sources are available doesn't mean they are used in compilation/runtime. These are merely extracted from the source jars for each dependency, as fetched by sbt. If you want to change and edit the external libraries you're using, *this is not the way*. Add them as an sbt project or manually to your own project as source files, to make any changes and compile. Again, this plugin only allows to quickly add the sources to the same Sublime window for convenience purposes only. Sbt doesn't compile *anything* in the `sublimeExternalSourceDirectoryName` folder!

* If you change any of the library dependencies or the specific settings detailed in [Configuration](https://github.com/orrsella/sbt-sublime#configuration), you'll need to reload the sbt project with the `reload` command, and then execute `gen-sublime` again. This will add/remove dependencies' sources accordingly, making sure the list in up-to-date.

* If you change the name of the external sources directory (`sublimeExternalSourceDirectoryName`), you might need to close and re-open the Sublime project for the change to take effect.

* All other Sublime project settings should remain intact when using the plugin, don't be afraid to tweak it if you want.

* Sources, as do dependencies, are usually appropriate for the `scalaVersion` you're using. Changing it and re-running the `gen-sublime` command will update sources accordingly.

* Consider adding the .sublime-project file to `.gitignore` and `file_exclude_patterns` (in sublime's preferences) to not commit and/or display the project file in Sublime, if it's saved to it's default location in the root folder.

## Feedback

Any comments/suggestions? Let me know what you think – I'd love to hear from you. Send pull requests, issues or contact me: [@orrsella](http://twitter.com/orrsella) and [orrsella.com](http://orrsella.com)

## License

This software is licensed under the Apache 2 license, quoted below.

Copyright (c) 2013 Orr Sella

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
