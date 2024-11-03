(ns moon.modifiers-test
  (:require [clojure.test :refer :all]
            [moon.modifiers :as mods]))

(deftest add-mods
  (is (= (mods/add {:modifier/movement-speed {:op/mult 10}}
                   {:modifier/movement-speed {:op/mult -10}})
         {:modifier/movement-speed {:op/mult 0}}))

  (is (= (mods/add {:modifier/strength {:op/inc 3}}
                   {:modifier/movement-speed {:op/mult -1}})
         {:modifier/strength {:op/inc 3}
          :modifier/movement-speed {:op/mult -1}})))

(deftest remove-mods
  (is (= (mods/remove {:modifier/movement-speed {:op/mult 50}}
                      {:modifier/movement-speed {:op/mult -10}})
         {:modifier/movement-speed {:op/mult 60}})))
