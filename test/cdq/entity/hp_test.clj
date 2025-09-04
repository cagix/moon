(ns cdq.entity.hp-test
  (:require [cdq.stats :as modifiers]
            [clojure.test :refer :all]))

(defn- ->value [hp-base-value hp-mods]
  (modifiers/get-hitpoints
   {:entity/hp hp-base-value
    :entity/modifiers {:modifier/hp-max hp-mods}}))

(deftest max-modifier
  (is (= (->value [100 100]
                  {:op/inc 1})
         [100 101]))

  (is (= (->value [100 100]
                  {:op/mult 10})
         [100 110]))

  (is (= (->value [100 100]
                  {:op/inc 10
                   :op/mult 50})
         [100 165]))

  (is (= (->value [100 100]
                  {:op/inc -10})
         [90 90]))

  (is (= (->value [100 100]
                  {:op/mult -50})
         [50 50]))

  (is (= (->value [100 100]
                  {:op/mult -50
                   :op/inc 200})
         [100 150])))
