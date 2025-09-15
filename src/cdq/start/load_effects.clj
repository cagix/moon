(ns cdq.start.load-effects
  (:require [cdq.effect :as effect]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.symbol :as symbol])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx edn-resource]
  (let [k->fn (-> edn-resource
                  io/resource
                  slurp
                  edn/read-string
                  symbol/require-resolve-symbols)]
    (extend APersistentVector
      effect/Effect
      {:applicable? (fn [{k 0 :as component} effect-ctx]
                      ((:applicable? (k->fn k)) component effect-ctx))

       :handle (fn [{k 0 :as component} effect-ctx ctx]
                 ((:handle (k->fn k)) component effect-ctx ctx))

       :useful? (fn [{k 0 :as component} effect-ctx ctx]
                  (if-let [f (:useful? (k->fn k))]
                    (f component effect-ctx ctx)
                    true))

       :render (fn [{k 0 :as component} effect-ctx ctx]
                 (if-let [f (:render (k->fn k))]
                   (f component effect-ctx ctx)
                   nil))}))
  ctx)
