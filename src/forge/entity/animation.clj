(ns forge.entity.animation
  (:require [anvil.animation :as animation]
            [anvil.world :refer [world-delta]]))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defn create [[_ animation] eid]
  (swap! eid assoc-image-current-frame animation))

(defn tick [[k animation] eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc k (animation/tick animation world-delta)))))
