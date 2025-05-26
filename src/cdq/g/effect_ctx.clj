(ns cdq.g.effect-ctx
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.vector2 :as v]
            gdl.application))

(extend-type gdl.application.Context
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/mouseover-eid] :as ctx}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (entity/position @mouseover-eid))
                              (g/world-mouse-position ctx))]
      {:effect/source eid
       :effect/target mouseover-eid
       :effect/target-position target-position
       :effect/target-direction (v/direction (entity/position @eid) target-position)}))

  (npc-effect-ctx [ctx eid]
    (let [entity @eid
          target (g/nearest-enemy ctx entity)
          target (when (and target
                            (g/line-of-sight? ctx entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (entity/position entity)
                                               (entity/position @target)))})))
