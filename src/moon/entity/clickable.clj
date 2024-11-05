(ns moon.entity.clickable
  (:require [gdl.graphics.text :as text]))

(defn render [{:keys [text]} {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (text/draw {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
