(ns ^:no-doc anvil.entity.delete-after-animation-stopped?
  (:require [clojure.component :as component :refer [defcomponent]]
            [gdl.graphics.animation :as animation]))

(defcomponent :entity/delete-after-animation-stopped?
  (component/create [_ eid c]
    (-> @eid :entity/animation :looping? not assert))

  (component/tick [_ eid c]
    (when (animation/stopped? (:entity/animation @eid))
      (swap! eid assoc :entity/destroyed? true))))
