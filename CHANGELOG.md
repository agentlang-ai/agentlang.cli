# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [TODO]
1. Support for multiple source paths (`:model-paths` in `model.fractl`)
2. Support for deploying apps
   - Deploy
   - Undeploy
   - Deployment status
3. Replace `git` command usage with JGit
   - One less package to install for end-user
4. Distribution with/out JRE (Linux x64/arm64, macOS silicon/x64, Windows x64)
   - Installer for Linux/macOS
   - GUI Installer for Windows

## [0.6.1] - 2024-11-??

- Fix `:fs` dependency resolution for clone use-case
- Baseline AgentLang version `0.6.1-alpha6`

## [0.6.0] - 2024-10-22

- Fix `bin/agent` script for symlink
- Enable script execution without a command
- Baseline AgentLang version `0.6.0` (GA)
- Documentation improvements

## [0.5.0] - 2024-10-18

- Support for running individual AgentLang scripts
  - Synopsis: `run [-c config.edn] [path/to/script.al]`
- Git branch and tag support in dependencies and clone-URI
  - As URI fragment identifier: `#<branch>`
  - As URI parameters: `?branch=<branch>` and `?tag=<tag>`
- Baseline AgentLang version `0.6.0-alpha4`
- Distribution support for Linux/macOS/Unix
  - Tarball with uberjar built-in
  - Expects Java21 and Git installed

## [0.4.0] - 2024-10-04

- Support for non-JAR dependencies
  - Local filesystem dependencies
  - Git dependencies
    - Inject Github token when env var defined: `GIT_DEPS_INJECT_TOKEN=true`
    - Use env vars `GITHUB_USERNAME` and `GITHUB_TOKEN` to inject token into Github URI
  - Retrofit commands to work with local/Git deps
    - `deps`, `depstree`, `classpath`
- Support for creating resolvers
  - Resolver template, using `agent new resolver <resolver-name>`
- AgentLang version
  - Baseline version `0.6.0-alpha3`

## [0.3.0] - 2024-09-26

- Rename project to agentlang.cli
  - Rename script to `agent`
  - Refactor/rename internal source/test files
- Target AgentLang (rather than Fractl)
  - Baseline version `0.6.0-alpha2`
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
