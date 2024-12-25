(ns anvil.effect
  (:require [anvil.component :as component]
            [cdq.context :as world]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn check-update-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [c {:keys [effect/source effect/target] :as ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (world/line-of-sight? c @source @target))
    ctx
    (dissoc ctx :effect/target)))

; TODO 'effect' with 'effect-components' which have a certain defsystem 'protocol'
; of required and optional defsystems
; so just do/applicable?/useful?/and grep 'effects', e.t.c. rename keywords?
; => so sound is just a component....

(defn filter-applicable? [ctx effect]
  (filter #(component/applicable? % ctx) effect))

(defn some-applicable? [ctx effect]
  (seq (filter-applicable? ctx effect)))

(defn applicable-and-useful? [ctx effect]
  (->> effect
       (filter-applicable? ctx)
       (some #(component/useful? % ctx))))

(defn do-all! [c ctx effect]
  (run! #(component/handle % ctx c)
        (filter-applicable? ctx effect)))

(defn render-info [c ctx effect]
  (run! #(component/render-effect % ctx c) effect))
