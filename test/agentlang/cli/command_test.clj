(ns agentlang.cli.command-test
  (:require [clojure.test :refer [deftest is testing]]
            [agentlang.cli.command :as command]
            [agentlang.cli.core :as core])
  (:import (clojure.lang ExceptionInfo)))


(declare thrown? thrown-with-msg?)


(deftest test-command-deps
  (testing "Missing model.al file"
    (is (= "File model.al does not exist"
           (core/model-dir-error ".")))
    (is (= 1 (command/command-deps "."))))
  (testing "Unspecified AgentLang version in model.al"
    (with-redefs [core/model-dir-error (fn [_])
                  core/read-model (fn [_] {})]
      (is (seq (command/command-deps "."))))))
