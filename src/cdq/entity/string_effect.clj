(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.timer :as timer]))

(defn draw [{:keys [text]} entity {:keys [ctx/world-unit-scale]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 world-unit-scale))
                  :scale 2
                  :up? true}]]))

(defn tick!
  [{:keys [counter]}
   eid
   {:keys [ctx/elapsed-time]}]
  (when (timer/stopped? elapsed-time counter)
    [[:tx/dissoc eid :entity/string-effect]]))
