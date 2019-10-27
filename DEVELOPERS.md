# Configuration of build

Consider changing the property deploy.anchor.distribution.path in pom.xml to a different path.

It points to the location of an anchor-distribution in which to copy the plugins and their libs when the deploy
phase is executed e.g.

> mvn deploy

# Steps for adding a new plugin module

1. create project in subdir
2. add module name to the pom.xml in the root folder
3. in anchor-plugins-gpl-assembly/pom.xml  add a dependency to the project
4. in anchor-plugins-gpl-assembly/src/assembly/dist.xml  add a line to the <includes> and <excludes>