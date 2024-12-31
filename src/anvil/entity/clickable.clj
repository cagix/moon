(ns ^:no-doc anvil.entity.clickable
  (:require [anvil.entity :as entity]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :entity/clickable
  (entity/render-default [[_ {:keys [text]}] {:keys [entity/mouseover?] :as entity} c]
    (when (and mouseover? text)
      (let [[x y] (:position entity)]
        (c/draw-text c
                     {:text text
                      :x x
                      :y (+ y (:half-height entity))
                      :up? true})))))
