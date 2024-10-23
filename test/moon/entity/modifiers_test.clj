(ns moon.entity.modifiers-test
  (:require [clojure.test :refer :all]
            [moon.tx :as tx]
            moon.val-max ; load val-max ops
            [moon.entity.modifiers :refer [modified-value]]
            moon.entity)) ; load :e/update

(deftest apply-modifiers
  (let [eid (atom {:entity/modifiers {:modifier/movement-speed {:op/mult [0.1]}}})]
    (tx/do! [[:tx/apply-modifiers
              eid
              {:modifier/movement-speed {:op/mult -0.1}}]])
    (is (= (:modifier/movement-speed (:entity/modifiers @eid))
           #:op{:mult [0.1 -0.1]}))))

(deftest reverse-modifiers
  (let [eid (atom {:entity/modifiers {:modifier/movement-speed {:op/mult [0.1 -0.1]}}})]
    (tx/do! [[:tx/reverse-modifiers
              eid
              {:modifier/movement-speed {:op/mult -0.1}}]])
    (is (= (:modifier/movement-speed (:entity/modifiers @eid))
           #:op{:mult [0.1]}))))

(deftest test-modified-value
  (let [->entity (fn [modifiers] {:entity/modifiers modifiers})]
    (is (= (modified-value (->entity {:modifier/damage-deal {:op/val-inc [30]
                                                             :op/val-mult [0.5]}})
                           :modifier/damage-deal
                           [5 10])
           [52 52]))

    (is (= (modified-value (->entity {:modifier/damage-deal {:op/val-inc [30]}
                                      :stats/fooz-barz {:op/babu [1 2 3]}})
                           :modifier/damage-deal
                           [5 10])
           [35 35]))

    (is (= (modified-value (->entity {}) :modifier/damage-deal [5 10])
           [5 10]))

    (is (= (modified-value (->entity {:modifier/hp {:op/max-inc [10 1]
                                                    :op/max-mult [0.5]}})
                           :modifier/hp
                           [100 100])
           [100 166]))

    (is (= (modified-value (->entity {:modifier/movement-speed {:op/inc [2]
                                                                :op/mult [0.1 0.2]}})
                           :modifier/movement-speed
                           3)
           6.5))))
