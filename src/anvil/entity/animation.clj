(ns ^:no-doc anvil.entity.animation
  (:require [anvil.component :as component]
            [clojure.utils :refer [defmethods]]
            [gdl.graphics.animation :as animation]))

(defmethods :entity/animation
  (component/create [[_ animation] eid c]
    (swap! eid assoc :entity/image (animation/current-frame animation)))

  (component/tick [[k animation] eid {:keys [cdq.context/delta-time]}]
    (swap! eid #(-> %
                    (assoc :entity/image (animation/current-frame animation))
                    (assoc k (animation/tick animation delta-time))))))
