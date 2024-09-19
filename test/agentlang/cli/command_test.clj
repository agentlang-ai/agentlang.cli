(ns agentlang.cli.command-test
  (:require [clojure.test :refer [deftest is testing]]
            [agentlang.cli.command :as command]
            [agentlang.cli.core :as core])
  (:import (clojure.lang ExceptionInfo)))


(deftest test-command-deps
  (testing "Missing model.fractl file"
    (is (= 1 (command/command-deps "."))))
  (testing "Unspecified Fractl version in model.fractl"
    (with-redefs [core/read-model (fn [_] {})]
      (is (seq (command/command-deps "."))))))
