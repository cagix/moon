(ns forge.entity.hp-test
  (:require [clojure.test :refer :all]
            [forge.modifiers :refer [hitpoints]]))

(deftest max-modifier
  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/inc 1}}})
         [100 101]))

  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/mult 10}}})
         [100 110]))

  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/inc 10
                                                          :op/mult 50}}})
         [100 165]))

  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/inc -10}}})
         [90 90]))

  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/mult -50}}})
         [50 50]))

  (is (= (hitpoints {:entity/hp [100 100]
                     :entity/modifiers {:modifier/hp-max {:op/mult -50
                                                          :op/inc 200}}})
         [100 150])))
