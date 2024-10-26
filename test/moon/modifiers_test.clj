(ns moon.modifiers-test
  (:require [clojure.test :refer :all]
            [moon.modifiers :as mods]))

(deftest add-mods
  (is (= (mods/add {:modifier/movement-speed {:op/mult [0.1]}}
                   {:modifier/movement-speed {:op/mult -0.1}})
         {:modifier/movement-speed {:op/mult [0.1 -0.1]}}))

  (is (= (mods/add {:modifier/strength {:op/inc [1 2]}}
                   {:modifier/movement-speed {:op/mult -0.1}})
         {:modifier/strength {:op/inc [1 2]}
          :modifier/movement-speed {:op/mult [-0.1]}})))

(deftest remove-mods
  (is (= (mods/remove {:modifier/movement-speed {:op/mult [0.1 0.5 -0.1]}}
                      {:modifier/movement-speed {:op/mult -0.1}})
         {:modifier/movement-speed {:op/mult [0.1 0.5]}})))

(deftest value-mods-conversion
  (is (= (mods/value-mods->mods {:modifier/foo {:op/bar 3
                                                :op/mult 5}
                                 :modifier/bar {:op/zuk 4}})
         #:modifier{:foo #:op{:bar [3],
                              :mult [5]},
                    :bar #:op{:zuk [4]}})))
