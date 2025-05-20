(ns cdq.tx.audiovisual
  (:require [cdq.tx.sound :as tx.sound]
            [cdq.tx.spawn-entity]))

(defn do! [{:keys [ctx/effect-body-props] :as ctx}
           position
           {:keys [tx/sound entity/animation]}]
  (tx.sound/do! ctx sound)
  (cdq.tx.spawn-entity/do! ctx
                           position
                           effect-body-props
                           {:entity/animation animation
                            :entity/delete-after-animation-stopped? true}))
