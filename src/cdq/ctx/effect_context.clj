(ns cdq.ctx.effect-context
  (:require [cdq.entity :as entity]
            [cdq.c :as c]
            [cdq.math.vector2 :as v]))

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
