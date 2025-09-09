(ns cdq.tx.audiovisual
  (:require [cdq.db :as db]))

(defn do! [[_ position audiovisual]
           {:keys [ctx/db]}]
  (let [{:keys [tx/sound
                entity/animation]} (if (keyword? audiovisual)
                                     (db/build db audiovisual)
                                     audiovisual)]
    [[:tx/sound sound]
     [:tx/spawn-effect
      position
      {:entity/animation animation
       :entity/delete-after-animation-stopped? true}]]))
