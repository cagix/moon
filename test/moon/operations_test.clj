(ns moon.operations-test
  (:require [clojure.test :refer :all]
            [moon.operations :as ops]))

(deftest add-and-remove
  (is (= (ops/add {:+ 6}
                  {:* -5 :+ -1})
         {:+ 5, :* -5}))

  (is (= (ops/remove {:+ 6 :* -50}
                     {:+ 2 :* -50})
         {:+ 4, :* 0})))

(deftest info-text
  (= (ops/info-text [[:op/inc -4] [:op/mult 24]]
                    "Strength")
     "-4 Strength\n+24% Strength"))

(deftest ops-apply
  (is (= (ops/apply {:op/inc 6 :op/mult 50} 10)
         24))

  (is (= (ops/apply {:op/inc -5 :op/mult 20} 10)
         6)))
