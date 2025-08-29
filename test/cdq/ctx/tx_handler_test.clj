(ns cdq.ctx.tx-handler-test
  (:require [cdq.game :refer [do! handle-txs!]]
            [clojure.test :refer :all]))

(defmethod do! ::foobar [_ ctx]
  [[::bar-baz]
   [::bim-bam]])

(defmethod do! ::bar-baz [_ {:keys [accum]}]
  (swap! accum conj 1)
  nil)

(defmethod do! ::bim-bam [_ {:keys [accum]}]
  (swap! accum conj 2)
  nil)

(deftest return-flat-txs
  (let [ctx {:accum (atom [])}
        result (handle-txs! ctx [[::foobar]])]
    (is (= result
           [[::foobar]
            [::bar-baz]
            [::bim-bam]]))
    (is (= @(:accum ctx)
           [1 2]))))
