(ns forge.entity.hp-test
  (:require [anvil.entity.hp :refer [->value]]
            [clojure.test :refer :all]))

(deftest max-modifier
  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/inc 1}}})
         [100 101]))

  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/mult 10}}})
         [100 110]))

  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/inc 10
                                                        :op/mult 50}}})
         [100 165]))

  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/inc -10}}})
         [90 90]))

  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/mult -50}}})
         [50 50]))

  (is (= (->value {:entity/hp [100 100]
                   :entity/modifiers {:modifier/hp-max {:op/mult -50
                                                        :op/inc 200}}})
         [100 150])))
