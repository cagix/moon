(ns cdq.tx.spawn-projectile
  (:require [cdq.ctx :as ctx]
            [cdq.vector2 :as v]
            [cdq.world :as world]))

(defn do! [{:keys [position direction faction]}
           {:keys [entity/image
                   projectile/max-range
                   projectile/speed
                   entity-effects
                   projectile/piercing?] :as projectile}]
  (let [size (world/projectile-size projectile)]
    (world/spawn-entity! ctx/world
                         position
                         {:width size
                          :height size
                          :z-order :z-order/flying
                          :rotation-angle (v/angle-from-vector direction)}
                         {:entity/movement {:direction direction
                                            :speed speed}
                          :entity/image image
                          :entity/faction faction
                          :entity/delete-after-duration (/ max-range speed)
                          :entity/destroy-audiovisual :audiovisuals/hit-wall
                          :entity/projectile-collision {:entity-effects entity-effects
                                                        :piercing? piercing?}})))
