(ns anvil.effect
  (:require [anvil.component :as component]
            [anvil.world :as world]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [{:keys [effect/source effect/target] :as ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? @source @target))
    ctx
    (dissoc ctx :effect/target)))

; TODO 'effect' with 'effect-components' which have a certain defsystem 'protocol'
; of required and optional defsystems
; so just do/applicable?/useful?/and grep 'effects', e.t.c. rename keywords?
; => so sound is just a component....

(defn filter-applicable? [ctx effects]
  (filter #(component/applicable? % ctx) effects))

(defn some-applicable? [ctx effects]
  (seq (filter-applicable? ctx effects)))

(defn do-all! [ctx effects]
  (run! #(component/handle % ctx)
        (filter-applicable? ctx effects)))
