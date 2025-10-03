(ns cdq.entity.state.active-skill.tick
  (:require [cdq.effect :as effect]
            [gdl.timer :as timer]
            [cdq.world.raycaster :as raycaster]))

(defn- update-effect-ctx
  [world {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (raycaster/line-of-sight? world @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn txs
  [{:keys [skill effect-ctx counter]}
   eid
   {:keys [world/elapsed-time]
    :as world}]
  (let [effect-ctx (update-effect-ctx world effect-ctx)]
    (cond
     (not (seq (filter #(effect/applicable? % effect-ctx)
                       (:skill/effects skill))))
     [[:tx/event eid :action-done]]

     (timer/stopped? elapsed-time counter)
     [[:tx/effect effect-ctx (:skill/effects skill)]
      [:tx/event eid :action-done]])))
