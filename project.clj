(defproject com.github.fractl-io/fractl.cli "0.1.0-SNAPSHOT"
  :description "CLI tool for Fractl applications"
  :url "https://github.com/fractl-io/fractl.cli"
  :license {:name "Apache License 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :dependencies [[org.clojure/clojure "1.12.0-rc1"]
                 [org.clojure/tools.cli "1.0.214"]
                 [clj-commons/pomegranate "1.2.24"
                  :exclusions [org.slf4j/jcl-over-slf4j org.slf4j/slf4j-api
                               org.apache.maven.wagon/wagon-provider-api
                               org.apache.httpcomponents/httpcore
                               org.apache.httpcomponents/httpclient]]
                 [org.slf4j/slf4j-nop "1.7.25"] ; wagon-http uses slf4j
                 [org.apache.maven.wagon/wagon-http "3.5.3"
                  :exclusions [org.slf4j/slf4j-api]]]
  :resource-paths ["target/resources"]
  ;:main ^:skip-aot fractl.cli.main
  :main fractl.cli.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:plugins [[lein-shell "0.5.0"]]
                   :project-edn {:output-file "resources/project.edn"}
                   :prep-tasks [["shell" "mkdir" "-p" "${:user-home}/.fractl/self-installs/"]]}}
  :plugins [[lein-project-edn "0.3.0"]]
  :project-edn {:output-file "target/resources/project.edn"}
  :hooks [leiningen.project-edn/activate]
  :user-home ~(System/getProperty "user.home")
  :aliases
  {"local"
   ["shell"
    "cp" "target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "${:user-home}/.fractl/self-installs/"]
   "native"
   ["shell"
    "native-image" "--report-unsupported-elements-at-runtime"
    "--initialize-at-build-time" "--no-server"
    "-jar" "target/uberjar/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:Name=./target/${:name}"]})
