(ns cdq.tx.audiovisual
  (:require [cdq.ctx :as ctx]
            [cdq.tx.sound :as tx.sound]
            [cdq.impl.world :as world]))

(defn do! [position {:keys [tx/sound entity/animation]}]
  (tx.sound/do! sound)
  (world/spawn-entity! position
                       ctx/effect-body-props
                       {:entity/animation animation
                        :entity/delete-after-animation-stopped? true}))
