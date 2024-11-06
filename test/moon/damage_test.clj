(ns moon.damage-test
  (:require [clojure.test :refer :all]
            [moon.damage :as damage]))

(def source
  {:entity/modifiers {:modifier/damage-deal-min {:op/inc 1
                                                 :op/mult 50}}})

(deftest modify-source-damage
  (= (damage/modified source {:damage/min-max [5 10]})
     #:damage{:min-max [9 10]}))

(def source
  {:entity/modifiers {:modifier/damage-deal-min {:op/inc 20}}})

(deftest modify-source-damage-align
  (= (damage/modified source {:damage/min-max [5 10]})
     #:damage{:min-max [25 25]}))

(def source
  {:entity/modifiers {:modifier/damage-deal-min {:op/mult -100}
                      :modifier/damage-deal-max {:op/inc 5}}})

(deftest modify-source-damage-align-negative
  (= (damage/modified source {:damage/min-max [5 10]})
     #:damage{:min-max [0 15]}))

(def target
  {:entity/modifiers {:modifier/damage-receive-max {:op/mult -50}}})

(deftest modify-damage
  (= (damage/modified source target {:damage/min-max [5 10]})
     #:damage{:min-max [0 7]}))

(deftest max-reduces-min
  (= (damage/modified {} target {:damage/min-max [5 5]})
     #:damage{:min-max [2 2]}))

(deftest info
  (= (damage/info {:damage/min-max [10 12]})
     "10-12 damage"))

(deftest max-decrease-lowers-min
  (= (damage/modified
      {:entity/modifiers {:modifier/damage-deal-max {:op/inc -5}}}
      {:damage/min-max [8 9]})
     {:damage/min-max [4 4]}))

(deftest min-increase-increases-max
  (= (damage/modified
      {:entity/modifiers {:modifier/damage-deal-min {:op/inc 5}}}
      {:damage/min-max [8 9]})
     {:damage/min-max [13 13]}))
