(ns moon.tx.projectile
  (:require [gdl.math.vector :as v]
            [moon.projectile :as projectile]
            [moon.world.entities :as entities]))

(defn handle [{:keys [position direction faction]}
              {:keys [entity/image
                      projectile/max-range
                      projectile/speed
                      entity-effects
                      projectile/piercing?] :as projectile}]
  (let [size (projectile/size projectile)]
    (entities/create position
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
                                                    :piercing? piercing?}})
    nil))
