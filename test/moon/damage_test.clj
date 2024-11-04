(ns moon.damage-test
  (:require [clojure.test :refer :all]
            [moon.damage :as damage]))

(def source
  {:entity/modifiers {:modifier/damage-deal {:op/val-inc 1
                                             :op/max-mult 100}}})

(deftest modify-source-damage
  (= (damage/modified source {:damage/min-max [5 10]})
     #:damage{:min-max [6 20]}))

(def target
  {:entity/modifiers {:modifier/damage-receive {:op/max-mult -50}}})

(deftest modify-damage
  (= (damage/modified source target {:damage/min-max [5 10]})
     #:damage{:min-max [6 10]}))

(deftest info
  (= (damage/info {:damage/min-max [10 12]})
     "10-12 damage"))
