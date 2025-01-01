(ns ^:no-doc anvil.entity.animation
  (:require [clojure.component :as component :refer [defcomponent]]
            [gdl.graphics.animation :as animation]))

(defcomponent :entity/animation
  (component/create [[_ animation] eid c]
    (swap! eid assoc :entity/image (animation/current-frame animation)))

  (component/tick [[k animation] eid {:keys [cdq.context/delta-time]}]
    (swap! eid #(-> %
                    (assoc :entity/image (animation/current-frame animation))
                    (assoc k (animation/tick animation delta-time))))))
