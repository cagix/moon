(ns cdq.stats.ops-test
  (:require [cdq.stats.ops :as ops]
            [clojure.test :refer :all]))

(deftest add-and-remove
  (is (= (ops/add {:+ 6}
                  {:* -5 :+ -1})
         {:+ 5, :* -5}))

  (is (= (ops/remove {:+ 6 :* -50}
                     {:+ 2 :* -50})
         {:+ 4, :* 0})))

(deftest info-texts
  (is (= (ops/info-text {:op/inc -4
                         :op/mult 24}
                        "Strength")
         "-4 Strength\n+24% Strength"))

  (is (= (ops/info-text {:op/inc -4
                         :op/mult 0}
                        "Strength")
         "-4 Strength"))

  (is (= (ops/info-text {:op/mult 35}
                        "Hitpoints")
         "+35% Hitpoints"))

  (is (= (ops/info-text {:op/inc -30
                         :op/mult 5}
                        "Hitpoints")
         "-30 Hitpoints\n+5% Hitpoints")))

(deftest apply-test
  (is (= (ops/apply {:op/inc 6
                     :op/mult 50}
                    10)
         24))

  (is (= (ops/apply {:op/inc -5
                     :op/mult 20}
                    10)
         6)))
