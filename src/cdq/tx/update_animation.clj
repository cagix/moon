(ns cdq.tx.update-animation
  (:require [cdq.animation :as animation]))

(defn do! [{:keys [ctx/delta-time]} eid animation]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc :entity/animation (animation/tick animation delta-time)))))
