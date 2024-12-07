(ns forge.entity.animation
  (:require [clojure.utils :refer [defmethods]]
            [forge.animation :as animation]
            [forge.entity :refer [create tick]]
            [forge.world.time :refer [world-delta]]))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (animation/current-frame animation)))

(defmethods :entity/animation
  (create [[_ animation] eid]
    (swap! eid assoc-image-current-frame animation))

  (tick [[k animation] eid]
    (swap! eid #(-> %
                    (assoc-image-current-frame animation)
                    (assoc k (animation/tick animation world-delta))))))
