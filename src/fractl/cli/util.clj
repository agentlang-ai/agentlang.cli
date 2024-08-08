(ns fractl.cli.util
  (:import (java.io BufferedReader)
           (java.time Duration)))

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
