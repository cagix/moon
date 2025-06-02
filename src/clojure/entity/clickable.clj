(ns clojure.entity.clickable
  (:require [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/clickable
  (entity/render-default! [[_ {:keys [text]}]
                           {:keys [entity/mouseover?] :as entity}
                           _ctx]
    (when (and mouseover? text)
      (let [[x y] (entity/position entity)]
        [[:draw/text {:text text
                      :x x
                      :y (+ y (:half-height entity))
                      :up? true}]]))))
