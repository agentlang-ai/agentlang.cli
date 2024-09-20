# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [TODO]
1. Support for Github based dependencies
2. Support for multiple source paths (`:model-paths` in `model.fractl`)
3. Resolver template, using `ftl new resolver <resolver-name>`

## [0.3.0] - 2024-09-??

- Rename project to agentlang.cli
  - Rename script to `agent`
  - Refactor/rename internal source/test files
- Target AgentLang (rather than Fractl)
  - Baseline version `0.6.0-alpha1`
- Update Clojure dependency to version 1.12.0

## [0.2.0] - 2024-09-05

- Cloning code repo and running as one command
  - `ftl clonenrepl`
  - `ftl clonerepl`
  - `ftl clonerun` 
- Improvement to version reporting
  - `ftl version [<format>]`
- Fix for ambiguous fractl version
  - Rewrite `current`, `:current` and `nil` as `0.5.4`
- Documentation fixes
- Docker image
  - Add parameterised `CMD` for running apps

## [0.1.0] - 2024-08-14
- Fetching Maven/Clojars dependencies
  - `ftl deps`
  - `ftl depstree`
  - `ftl classpath`
- Running Fractl app with dependencies
  - `ftl run`
- Docker support
