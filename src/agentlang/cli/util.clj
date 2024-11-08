(ns agentlang.cli.util
  (:require [clojure.string :as string])
  (:import (java.io BufferedReader File)))


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


(def project-name-allowed-delims #{\- \_})


(defn project-name->component-dirname [^String project-name]
  (string/replace project-name #"-" "_"))


(defn invalid-project-name? [^String s]
  (when-not (and (Character/isLetter ^char (first s))
                 (Character/isLetter ^char (last s))
                 (->> (seq s)
                      (every? #(or (Character/isLetter ^char %)
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


(def file-separator File/separator)
(def path-separator File/pathSeparator)


(defn parse-uri-params [params-string]
  (->> (string/split params-string #"&")
       (reduce (fn [m pair]
                 (->> (string/split pair #"=")
                      (apply assoc m)))
               {})))


(defn parse-repo-uri
  "Parse a (Git) repo to determine the base repo URI, branch and tag.
  The following URI suffixes are supported:
  Suffix format     Example       Result
  -------------     -------       ------
  #<branch>         #foo          {:repo-branch \"foo\" ...}
  ?branch=<branch>  ?branch=foo   {:repo-branch \"foo\" ...}
  ?tag=<tag>        ?tag=v0.1     {:repo-tag \"v0.1\"   ...}
  Result
  {:repo-uri    ...
   :repo-branch ...
   :repo-tag    ...}"
  [repo-uri]
  (let [[repo-uri branch-name] (string/split repo-uri #"#" 2)
        [repo-uri qmark-suffix] (string/split repo-uri #"\?" 2)
        [branch-name tag-name] (if (some? qmark-suffix)
                                 (let [uri-params (parse-uri-params qmark-suffix)]
                                   [(or branch-name
                                        (get uri-params "branch")) (get uri-params "tag")])
                                 [branch-name nil])]
    {:repo-uri repo-uri
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
