(ns moon.operation-test
  (:require [clojure.test :refer :all]
            [moon.operation :as op]))

(deftest k->parts
  (is (= (#'op/k->parts :op/val-inc) [:val :op/inc])))

(deftest val-max-ops
  (is (= (op/apply [:op/val-inc 30]   [5 10]) [35 35]))
  (is (= (op/apply [:op/max-mult -50] [5 10]) [5 5]))
  (is (= (op/apply [:op/val-mult 200] [5 10]) [15 15]))
  (is (= (op/apply [:op/val-mult 130] [5 10]) [11 11]))
  (is (= (op/apply [:op/max-mult -80] [5 10]) [2 2]))
  (is (= (op/apply [:op/max-mult -90] [5 10]) [1 1])))
