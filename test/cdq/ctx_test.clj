(ns cdq.ctx-test
  (:require [cdq.ctx :as ctx]
            [clojure.test :refer :all]))

(deftest return-flat-txs
  (let [ctx {:accum (atom [])
             :ctx/txs-fn-map {:tx/foobar (fn [_ctx]
                                           [[:tx/bar-baz]
                                            [:tx/bim-bam]])
                              :tx/bar-baz (fn [{:keys [accum]}]
                                            (swap! accum conj 1)
                                            nil)
                              :tx/bim-bam (fn [{:keys [accum]}]
                                            (swap! accum conj 2)
                                            nil)}}
        result (ctx/handle-txs! ctx [[:tx/foobar]])]
    (is (= result
           [[:tx/foobar]
            [:tx/bar-baz]
            [:tx/bim-bam]]))
    (is (= @(:accum ctx)
           [1 2]))))
