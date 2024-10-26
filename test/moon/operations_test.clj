(ns moon.operations-test
  (:require [clojure.test :refer :all]
            [moon.operations :as ops]))

(deftest add-and-remove
  (is (= (ops/add {:+ [1 2 3]}
                  {:* -0.5 :+ -1})
         {:+ [1 2 3 -1], :* [-0.5]}))

  (is (= (ops/remove {:+ [1 2 3] :* [-0.5]}
                     {:+ 2 :* -0.5})
         {:+ [1 3], :* []})))

(deftest sum-vals
  (is (= (ops/sum-vals {:op/inc [1 2 3 -10 ]
                        :op/mult [-0.1 3 -0.5]})
         [[:op/inc -4] [:op/mult 2.4]])))

(deftest info-text
  (= (ops/info-text [[:op/inc -4] [:op/mult 2.4]]
                    "Strength")
     "-4 Strength\n+240% Strength"))

(deftest ops-apply
  (is (= (ops/apply {:op/inc 5 :op/mult 0.3} 10)
         19.5))

  (is (= (ops/apply {:op/inc -5 :op/mult 1} 10)
         10)))
