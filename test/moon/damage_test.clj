(ns moon.damage-test
  (:require [clojure.test :refer :all]
            [moon.damage :as damage]))

(deftest modify-source-damage
  (is (= (damage/modified {:entity/modifiers {:modifier/damage-deal-min {:op/inc 1
                                                                         :op/mult 50}}}
                          {:damage/min-max [5 10]})
         #:damage{:min-max [9 10]})))

(deftest modify-source-damage-align
  (is (= (damage/modified {:entity/modifiers {:modifier/damage-deal-min {:op/inc 20}}}
                          {:damage/min-max [5 10]})
         #:damage{:min-max [25 25]})))

(deftest modify-source-damage-align-negative
  (is (= (damage/modified {:entity/modifiers {:modifier/damage-deal-min {:op/mult -100}
                                              :modifier/damage-deal-max {:op/inc 5}}}
                          {:damage/min-max [5 10]})
         #:damage{:min-max [0 15]})))

(deftest modify-damage
  (is (= (damage/modified {:entity/modifiers {:modifier/damage-deal-min {:op/mult -100}
                                              :modifier/damage-deal-max {:op/inc 5}}}
                          {:entity/modifiers {:modifier/damage-receive-max {:op/mult -50}}}
                          {:damage/min-max [5 10]})
         #:damage{:min-max [0 7]})))

(deftest max-reduces-min
  (is (= (damage/modified {}
                          {:entity/modifiers {:modifier/damage-receive-max {:op/mult -50}}}
                          {:damage/min-max [5 5]})
         #:damage{:min-max [2 2]})))

(deftest max-decrease-lowers-min
  (is (= (damage/modified
          {:entity/modifiers {:modifier/damage-deal-max {:op/inc -5}}}
          {:damage/min-max [8 9]})
         {:damage/min-max [4 4]})))

(deftest min-increase-increases-max
  (is (= (damage/modified
          {:entity/modifiers {:modifier/damage-deal-min {:op/inc 5}}}
          {:damage/min-max [8 9]})
         {:damage/min-max [13 13]})))

(deftest does-not-reduce-below-zero
  (is (= (damage/modified
          {:entity/modifiers {:modifier/damage-deal-min {:op/inc -6}}}
          {:damage/min-max [5 10]})
         #:damage{:min-max [0 10]})))
