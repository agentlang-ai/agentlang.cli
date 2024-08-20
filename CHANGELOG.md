# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [TODO]
1. Support for Github based dependencies
2. Support for multiple source paths (`:model-paths` in `model.fractl`)
3. Resolver template, using `ftl new resolver <resolver-name>`

## [0.2.0] - 2024-08-??

- Cloning code repo and running as one command
  - `ftl clonerun`
- Improvement to version reporting
  - `ftl version [<format>]`
- Documentation fixes

## [0.1.0] - 2024-08-14
- Fetching Maven/Clojars dependencies
  - `ftl deps`
  - `ftl depstree`
  - `ftl classpath`
- Running Fractl app with dependencies
  - `ftl run`
- Docker support
