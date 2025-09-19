(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]))

(defn draw [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:graphics/world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))

(defn tick!
  [{:keys [counter]}
   eid
   {:keys [ctx/world]}]
  (when (timer/stopped? (:world/elapsed-time world) counter)
    [[:tx/dissoc eid :entity/string-effect]]))
