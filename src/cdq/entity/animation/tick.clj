(ns cdq.entity.animation.tick
  (:require [cdq.entity.animation :as animation]))

(defn txs [animation eid {:keys [world/delta-time]}]
  [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]
   (when (and (:delete-after-stopped? animation)
              (animation/stopped? animation))
     [:tx/mark-destroyed eid])])
