(ns cdq.tx.spawn-projectile
  (:require [cdq.ctx.effect-handler :refer [do!]]
            [cdq.projectile :as projectile]
            [cdq.vector2 :as v]
            [cdq.world :as world]))

(defmethod do! :tx/spawn-projectile [[_
                                      {:keys [position direction faction]}
                                      {:keys [entity/image
                                              projectile/max-range
                                              projectile/speed
                                              entity-effects
                                              projectile/piercing?] :as projectile}]
                                     ctx]
  (let [size (projectile/size projectile)]
    (world/spawn-entity! ctx
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
