(ns ^:no-doc anvil.entity.clickable
  (:require [anvil.component :as component]
            [gdl.graphics :as g]))

(defmethods :entity/clickable
  (component/render-default [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity}]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (g/draw-text {:text text
                      :x x
                      :y (+ y (:half-height entity))
                      :up? true})))))
