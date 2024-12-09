(ns forge.entity.string-effect
  (:require [anvil.graphics :as g]
            [anvil.time :refer [stopped?]]))

(defn tick [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defn render-above [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (g/pixels->world-units 5))
                  :scale 2
                  :up? true})))
