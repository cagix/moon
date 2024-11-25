(ns ^:no-doc moon.entity.animation
  (:require [forge.animation :as animation]
            [forge.system :refer [*k*]]
            [moon.world :as world]))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defn create [animation eid]
  (swap! eid assoc-image-current-frame animation))

(defn tick [animation eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc *k* (animation/tick animation world/delta)))))
