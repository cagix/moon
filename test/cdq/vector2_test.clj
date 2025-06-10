(ns cdq.vector2-test
  (:require [gdl.math.vector2 :as v]
            [clojure.test :refer :all]))

(deftest scale
  (is (= (v/scale [1 3] 0.5)
         [0.5 1.5]))
  (is (= (v/scale [2 1.2] -3)
         [-6.0 -3.6000001])))

