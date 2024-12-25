(ns ^:no-doc anvil.entity.animation
  (:require [anvil.component :as component]
            [anvil.world :as world]
            [gdl.graphics.animation :as animation]))

(defmethods :entity/animation
  (component/create [[_ animation] eid]
    (swap! eid assoc :entity/image (animation/current-frame animation)))

  (component/tick [[k animation] eid c]
    (swap! eid #(-> %
                    (assoc :entity/image (animation/current-frame animation))
                    (assoc k (animation/tick animation world/delta-time))))))
