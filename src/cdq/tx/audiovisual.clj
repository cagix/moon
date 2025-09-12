(ns cdq.tx.audiovisual
  (:require [cdq.ctx.db :as db]))

(defn do!
  [{:keys [ctx/db]}
   position
   audiovisual]
  (let [{:keys [tx/sound
                entity/animation]} (if (keyword? audiovisual)
                                     (db/build db audiovisual)
                                     audiovisual)]
    [[:tx/sound sound]
     [:tx/spawn-effect
      position
      {:entity/animation animation
       :entity/delete-after-animation-stopped? true}]]))
