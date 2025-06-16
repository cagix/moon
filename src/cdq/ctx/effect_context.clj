(ns cdq.ctx.effect-context
  (:require [cdq.entity :as entity]
            [cdq.ctx :as ctx]
            [cdq.w :as w]
            [gdl.c :as c]
            [gdl.math.vector2 :as v]))

(defn player-effect-ctx [{:keys [ctx/world]
                          :as ctx}
                         eid]
  (let [mouseover-eid (:world/mouseover-eid world)
        target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            (c/world-mouse-position ctx))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @eid) target-position)}))

(defn npc-effect-ctx [{:keys [ctx/world] :as ctx} eid]
  (let [entity @eid
        target (ctx/nearest-enemy ctx entity)
        target (when (and target
                          (w/line-of-sight? world entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))
