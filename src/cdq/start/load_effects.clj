(ns cdq.start.load-effects
  (:require [cdq.effect :as effect])
  (:import (clojure.lang APersistentVector)))

(defn do! [ctx k->fn]
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
                 nil))})
  ctx)
