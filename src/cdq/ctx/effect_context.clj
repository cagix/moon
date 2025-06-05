(ns cdq.ctx.effect-context
  (:require [cdq.entity :as entity]
            [cdq.vector2 :as v]
            [cdq.ctx :as ctx]
            [gdl.c :as c]))

(defn player-effect-ctx [{:keys [ctx/mouseover-eid]
                          :as ctx}
                         eid]
  (let [target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            (c/world-mouse-position ctx))]
    {:effect/source eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @eid) target-position)}))

(defn npc-effect-ctx [ctx eid]
  (let [entity @eid
        target (ctx/nearest-enemy ctx entity)
        target (when (and target
                          (ctx/line-of-sight? ctx entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))
