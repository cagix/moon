(ns cdq.entity.string-effect
  (:require [gdl.context :as c]
            [gdl.context.timer :as timer]))

(defn tick [[k {:keys [counter]}] eid c]
  (when (timer/stopped? c counter)
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
