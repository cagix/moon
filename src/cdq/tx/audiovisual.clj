(ns cdq.tx.audiovisual
  (:require [cdq.ctx :as ctx]
            [cdq.tx.sound :as tx.sound]
            [cdq.tx.spawn-entity]))

(defn do! [position {:keys [tx/sound entity/animation]}]
  (tx.sound/do! sound)
  (cdq.tx.spawn-entity/do! position
                           ctx/effect-body-props
                           {:entity/animation animation
                            :entity/delete-after-animation-stopped? true}))
