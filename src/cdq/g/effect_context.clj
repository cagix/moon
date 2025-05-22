(ns cdq.g.effect-context
  (:require [cdq.g :as g]
            [cdq.vector2 :as v]
            [gdl.graphics.viewport :as viewport]))

(extend-type cdq.g.Game
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/mouseover-eid
                              ctx/world-viewport]}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (:position @mouseover-eid))
                              (viewport/mouse-position world-viewport))]
      {:effect/source eid
       :effect/target mouseover-eid
       :effect/target-position target-position
       :effect/target-direction (v/direction (:position @eid) target-position)}))

  (npc-effect-ctx [ctx eid]
    (let [entity @eid
          target (g/nearest-enemy ctx entity)
          target (when (and target
                            (g/line-of-sight? ctx entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (:position entity)
                                               (:position @target)))})))
