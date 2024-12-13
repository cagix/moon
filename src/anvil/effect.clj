(ns anvil.effect
  (:require [anvil.world :as world]
            [gdl.utils :refer [defsystem]]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? @source @target))
    ctx
    (dissoc ctx :effect/target)))

(defsystem applicable?)

(defsystem handle)

(defn filter-applicable? [ctx effects]
  (filter #(applicable? % ctx) effects))

(defn some-applicable? [ctx effects]
  (seq (filter-applicable? ctx effects)))

(defn do-all! [ctx effects]
  (run! #(handle % ctx)
        (filter-applicable? ctx effects)))
