(ns cdq.math.vector2-test
  (:require [cdq.math.utils :as math-utils]
            [cdq.gdx.math.vector2 :as v]
            [clojure.test :refer :all]))

(set! *unchecked-math* :warn-on-boxed)

(deftest nearly-equal?
  (is (v/nearly-equal? [0.0000011 0.0123]
                       [0.000001 0.0123])))

(deftest scale
  (is (v/nearly-equal? (v/scale [1 3] 0.5)
                       [0.5 1.5]))
  (is (v/nearly-equal? (v/scale [2 1.2] -3)
                       [-6.0 -3.6000001]))
  (is (v/nearly-equal? (v/scale [0 0] 10)
                       [0.0 0.0])))

(deftest length
  (is (math-utils/nearly-equal? (v/length [1.2 0.1])
                                1.2041595))
  (is (math-utils/nearly-equal? (v/length [1.2 -0.1])
                                1.2041595)))
