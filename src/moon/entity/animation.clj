(ns moon.entity.animation
  (:require [gdl.animation :as animation]
            [gdl.system :refer [*k*]]
            [moon.world.time :as time]))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defn create [animation eid]
  (swap! eid assoc-image-current-frame animation)
  nil)

(defn tick [animation eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc *k* (animation/tick animation time/delta))))
  nil)
