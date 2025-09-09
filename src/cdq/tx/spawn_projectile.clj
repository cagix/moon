(ns cdq.tx.spawn-projectile
  (:require [cdq.gdx.math.vector2 :as v]))

(defn do!
  [[_
    {:keys [position direction faction]}
    {:keys [entity/image
            projectile/max-range
            projectile/speed
            entity-effects
            projectile/size
            projectile/piercing?] :as projectile}]
   _ctx]
  [[:tx/spawn-entity
    {:entity/body {:position position
                   :width size
                   :height size
                   :z-order :z-order/flying
                   :rotation-angle (v/angle-from-vector direction)}
     :entity/movement {:direction direction
                       :speed speed}
     :entity/image image
     :entity/faction faction
     :entity/delete-after-duration (/ max-range speed)
     :entity/destroy-audiovisual :audiovisuals/hit-wall
     :entity/projectile-collision {:entity-effects entity-effects
                                   :piercing? piercing?}}]])
