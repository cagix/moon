(ns cdq.entity.state.active-skill
  (:require [cdq.effect :as effect]
            [cdq.timer :as timer]
            [cdq.raycaster :as raycaster]))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [raycaster {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (raycaster/line-of-sight? raycaster @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn tick! [{:keys [skill effect-ctx counter]}
             eid
             {:keys [ctx/elapsed-time
                     ctx/raycaster]}]
  (cond
   (not (effect/some-applicable? (update-effect-ctx raycaster effect-ctx) ; TODO how 2 test
                                 (:skill/effects skill)))
   [[:tx/event eid :action-done]
    ; TODO some sound ?
    ]

   (timer/stopped? elapsed-time counter)
   [[:tx/effect effect-ctx (:skill/effects skill)]
    [:tx/event eid :action-done]]))
