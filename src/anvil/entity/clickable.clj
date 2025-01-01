(ns ^:no-doc anvil.entity.clickable
  (:require [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :entity/clickable
  (component/render-default [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (c/draw-text c
                     {:text text
                      :x x
                      :y (+ y (:half-height entity))
                      :up? true})))))
