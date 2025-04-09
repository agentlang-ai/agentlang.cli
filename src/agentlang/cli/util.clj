(ns agentlang.cli.util
  (:require [clojure.string :as string]
            [clojure.java.shell :refer [sh]]
            [clojure.walk :as walk]
            [agentlang.cli.constant :as const])
  (:import (java.io BufferedReader File)
           (java.nio.file Paths)))


(set! *warn-on-reflection* true)


(defn throw-ex-info [message data]
  (throw (ex-info message data)))


(defmacro retry-if-interrupted
  [& body]
  `(loop []
     (let [[result# retry?#] (try
                               [(do ~@body) false]
                               (catch InterruptedException _#
                                 (.interrupt (Thread/currentThread))
                                 [nil true]))]
       (if retry?#
         (recur)
         result#))))


(defn apply-err-out
  [f & args]
  (binding [*out* *err*]
    (apply f args)))


(defn print-buffer
  "Print the content of supplied BufferedReader. Return true if it printed
  from the buffer, false otherwise."
  [^BufferedReader buffered-reader]
  (if (.ready buffered-reader)
    (do
      (print (slurp buffered-reader))
      (flush)
      true)
    false))


(defn sleep-millis [millis]
  (try
    (Thread/sleep ^long millis)
    (catch InterruptedException _
      (.interrupt (Thread/currentThread)))))


(defn err-print [msg & more]
  (apply apply-err-out print msg more))


(defn err-println [msg & more]
  (apply apply-err-out println msg more))


(defn err-prn [msg & more]
  (apply apply-err-out prn msg more))


(defn err-exit
  ([]
   (System/exit 1))
  ([msg & more]
   (apply err-println msg more)
   (System/exit 1)))


(defn as-string [x]
  (if (keyword? x)
    (name x)
    (str x)))


(def file-separator File/separator)
(def path-separator File/pathSeparator)
(def project-name-allowed-delims #{\- \_})


(defn camel-to-underscore [^String given-name]
  (->> (partition 2 1 given-name)  ; "HiThere" -> ((\H \i) (\i \T) (\T \h) (\h \e) (\e \r) (\r \e))
       (map (fn [[^char prev ^char each]]
              (if (and (Character/isLowerCase prev)
                       (Character/isUpperCase each))
                (str "_" each)
                each)))
       (cons (first given-name))
       string/join))


(defn project-name->component-dirname [^String project-name]
  (-> project-name
      camel-to-underscore
      string/lower-case
      (string/replace #"-" "_")))


(defn component-name->file-name [component-name]
  (let [^String component-name (as-string component-name)] ; e.g. :MicrosoftTeams.Core
    (str (->> (string/split component-name #"\.")
              (map project-name->component-dirname)
              (string/join file-separator))
         const/al-file-extension)))


(defn invalid-project-name? [^String s]
  (when-not (and (Character/isLetter ^char (first s))
                 (Character/isLetter ^char (last s))
                 (->> (seq s)
                      (every? #(or (Character/isLetter ^char %)
                                   (Character/isDigit ^char %)
                                   (get project-name-allowed-delims %)))))
    "Project-name may only have letters or dash/underscore between letters"))


(defn project-name->component-keyword [^String project-name]
  (->> (seq project-name)
       (reduce (fn [[s break?] ch]
                 (if (project-name-allowed-delims ch)
                   [s true]    ; exclude delimiter from component-name
                   [(str s (if break?
                             (string/upper-case ch)
                             ch))
                    false]))
               ["" true])
       first
       keyword))


(defn file-exists? [^String filepath]
  (-> (File. filepath)
      .exists))


(defn file-type [^String filepath]
  (let [f (File. filepath)]
    (cond
      (.isDirectory f) "directory"
      (.isFile f) "file")))


(defn parse-uri-params [params-string]
  (->> (string/split params-string #"&")
       (reduce (fn [m pair]
                 (->> (string/split pair #"=")
                      (apply assoc m)))
               {})))


(defn parse-repo-uri
  "Parse a (Git) repo to determine the base repo URI, path, branch and tag.
  Given the following Git repo URI format:

  https://<provider>.com/<org-name>/<repo-name>.git[/path/to/module][?<param>=<value>][#<branch>]
  git@<provider>.com:<org-name>/<repo-name>.git[/path/to/module][?<param>=<value>][#<branch>]

  Examples:
  https://github.com/fractl-io/camel-xml-template-resolver.git?branch=use-camel-xml-loader (git repo with branch)
  https://github.com/agentlang-hub/kamelets-xml.git/avro-deserialize-action (git repo with module path)

  The following URI suffixes are supported:
  Suffix format     Example       Result
  -------------     -------       ------
  /<module-path>    /avro/serde   {:repo-path \"/avro/serde\"}
  #<branch>         #foo          {:repo-branch \"foo\" ...}
  ?branch=<branch>  ?branch=foo   {:repo-branch \"foo\" ...}
  ?tag=<tag>        ?tag=v0.1     {:repo-tag \"v0.1\"   ...}
  Result
  {:repo-uri    ...
   :repo-path   ...
   :repo-branch ...
   :repo-tag    ...}"
  [repo-uri]
  (let [[repo-uri branch-name] (string/split repo-uri #"#" 2)
        [repo-uri qmark-suffix] (string/split repo-uri #"\?" 2)
        [repo-uri repo-path] (string/split repo-uri
                                           #"(?<=\.git)" ; positive-lookbehind regex, result repo-uri includes '.git'
                                           2)            ; result repo-path may be "" when unspecified
        [branch-name tag-name] (if (some? qmark-suffix)
                                 (let [uri-params (parse-uri-params qmark-suffix)]
                                   [(or branch-name
                                        (get uri-params "branch")) (get uri-params "tag")])
                                 [branch-name nil])]
    {:repo-uri repo-uri
     :repo-path repo-path
     :repo-branch branch-name
     :repo-tag tag-name}))


(defn git-repo-uri->repo-name [repo-uri]
  (let [last-name (-> (parse-repo-uri repo-uri)
                      :repo-uri
                      (string/split #"/")
                      last)]
    (if (string/ends-with? last-name ".git")
      (subs last-name 0
            (- (count last-name) 4))
      last-name)))


(defn move-lib [repo-dir]
  (let [{:keys [exit _ err]} (sh "mv" "lib" repo-dir)]
    (if (zero? exit)
      (println "Libraries loaded")
      (println "Error moving file:" err))))


(defn conj-some
  [coll item]
  (if (some? item)
    (conj coll item)
    coll))


(def ^:const windows? (-> (System/getProperty "os.name")
                          string/lower-case
                          (string/starts-with? "win")))


(defn absolute-file-path?
  [path]
  (if windows?
    (or (string/starts-with? path "\\")
        (string/starts-with? (subs path 1) ":\\"))
    (string/starts-with? path "/")))


(defn make-absolute-file-path
  [^String relative-path]
  (-> relative-path
      (Paths/get (make-array String 0))
      (.toAbsolutePath)
      (.normalize)
      str))


(defn make-parent-path
  [^String path]
  (-> (File. path)
      (.getParent)))


(defn prn-> [x & more]
  (prn x)
  x)


(defn prn->> [x & more]
  (let [y (->> more
               (cons x)
               last)]
    (prn y)
    y))


(defn- read-env-var-helper [x]
  (let [v
        (cond
          (symbol? x)
          (when-let [v (System/getenv (name x))]
            (let [s (try
                      (read-string v)
                      (catch Exception _e v))]
              (cond
                (not= (str s) v) v
                (symbol? s) (str s)
                :else s)))

          (vector? x)
          (first (filter identity (mapv read-env-var-helper x)))

          :else x)]
    v))


(defn read-env-var [x]
  (let [v (read-env-var-helper x)]
    (when (not v)
      (throw (Exception. (str "Environment variable " x " is not set."))))
    v))


(defn- env-var-call? [v]
  (and (list? v) (= 'env (first v))))


(defn- process-env-var-calls [config]
  (walk/prewalk
   #(if (map? %)
      (into {} (mapv (fn [[k v]]
                       [k (if (env-var-call? v)
                            (let [[n default] (rest v)]
                              (or (System/getenv (name n)) default))
                            v)])
                     %))
      %)
   config))


(defn read-config-file [config-file]
  (process-env-var-calls
   (binding [*data-readers* {'$ read-env-var}]
     (let [env-config (or (System/getenv "AGENT_CONFIG") "nil")]
       (merge (read-string (slurp config-file))
              (read-string env-config))))))


(defn get-ui-options [config]
  (let [build-config (:client (:build config))
        port (get-in config [:service :port] 8080)
        api-host (get build-config :api-host (str "http://localhost:" port))
        auth-url (get build-config :auth-url (str api-host "/auth"))
        auth-service (get-in config [:authentication :service] :none)]
    {:api-host api-host :auth-url auth-url :auth-service auth-service}))

(defn exec-in-shell [message commands] 
  (when message (println message))
  (let [res (apply sh commands)
        exit (:exit res)]
    (when-not (= 0 exit)
      (println "Command returned with non-zero result: " commands)
      (println (:out res))
      (System/exit 1))))
