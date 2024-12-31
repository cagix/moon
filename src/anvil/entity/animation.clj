(ns ^:no-doc anvil.entity.animation
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [defmethods]]
            [gdl.graphics.animation :as animation]))

(defmethods :entity/animation
  (entity/create [[_ animation] eid c]
    (swap! eid assoc :entity/image (animation/current-frame animation)))

  (entity/tick [[k animation] eid {:keys [cdq.context/delta-time]}]
    (swap! eid #(-> %
                    (assoc :entity/image (animation/current-frame animation))
                    (assoc k (animation/tick animation delta-time))))))
