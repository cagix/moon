(ns ^:no-doc moon.entity.clickable
  (:require [forge.app :refer [draw-text]]))

(defn render [{:keys [text]} {:keys [entity/mouseover?] :as entity}]
  (when (and mouseover? text)
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y (:half-height entity))
                  :up? true}))))
