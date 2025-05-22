(ns cdq.tx.audiovisual
  (:require [cdq.tx.sound :as tx.sound]
            [cdq.g :as g]))

(defn do! [ctx
           position
           {:keys [tx/sound entity/animation]}]
  (tx.sound/do! ctx sound)
  (g/spawn-effect! ctx
                   position
                   {:entity/animation animation
                    :entity/delete-after-animation-stopped? true}))
