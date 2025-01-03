(ns cdq.entity.string-effect
  (:require [cdq.context :refer [stopped?]]
            [gdl.context :as c]))

(defn tick [[k {:keys [counter]}] eid c]
  (when (stopped? c counter)
    (swap! eid dissoc k)))

(defn render-above [[_ {:keys [text]}] entity c]
  (let [[x y] (:position entity)]
    (c/draw-text c
                 {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (c/pixels->world-units c 5))
                  :scale 2
                  :up? true})))
