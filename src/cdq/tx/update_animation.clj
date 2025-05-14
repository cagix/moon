(ns cdq.tx.update-animation
  (:require [cdq.animation :as animation]
            [cdq.ctx :as ctx]))

(defn do! [eid animation]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc :entity/animation (animation/tick animation ctx/delta-time)))))
