(ns fractl.cli.util
  (:require [clojure.string :as string])
  (:import (java.io BufferedReader File)))


(set! *warn-on-reflection* true)


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
