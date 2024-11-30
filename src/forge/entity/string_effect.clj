(ns ^:no-doc forge.entity.string-effect
  (:require [forge.graphics :refer [draw-text pixels->world-units]]
            [moon.world :refer [stopped?]]))

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)))

(defn render-above [{:keys [text]} entity]
  (let [[x y] (:position entity)]
    (draw-text {:text text
                :x x
                :y (+ y (:half-height entity) (pixels->world-units 5))
                :scale 2
                :up? true})))


