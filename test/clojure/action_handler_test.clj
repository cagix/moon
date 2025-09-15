(ns clojure.action-handler-test
  (:require [clojure.action-handler :refer [handle-txs!]]
            [clojure.test :refer :all]))

(deftest return-flat-txs
  (let [ctx {:accum (atom [])}
        txs-fn-map {:tx/foobar (fn [_ctx]
                                 [[:tx/bar-baz]
                                  [:tx/bim-bam]])
                    :tx/bar-baz (fn [{:keys [accum]}]
                                  (swap! accum conj 1)
                                  nil)
                    :tx/bim-bam (fn [{:keys [accum]}]
                                  (swap! accum conj 2)
                                  nil)}
        result (handle-txs! txs-fn-map ctx [[:tx/foobar]])]
    (is (= result
           [[:tx/foobar]
            [:tx/bar-baz]
            [:tx/bim-bam]]))
    (is (= @(:accum ctx)
           [1 2]))))
