(ns ^:no-doc anvil.entity.string-effect
  (:require [anvil.component :as component]
            [anvil.world :refer [stopped?]]
            [gdl.graphics :as g]
            [gdl.utils :refer [defmethods]]))

(defmethods :entity/string-effect
  (component/tick [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)))

  (component/render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (g/draw-text {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (g/pixels->world-units 5))
                    :scale 2
                    :up? true}))))
