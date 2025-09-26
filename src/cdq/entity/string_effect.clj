(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]))

(defn draw [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:graphics/world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))
