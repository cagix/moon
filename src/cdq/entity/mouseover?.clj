(ns cdq.entity.mouseover?
  (:require [cdq.entity :as entity]
            [gdl.context :as c]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defn render-below [_
                    {:keys [entity/faction] :as entity}
                    {:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid]
    (c/with-line-width c 3
      #(c/ellipse c
                  (:position entity)
                  (:half-width entity)
                  (:half-height entity)
                  (cond (= faction (entity/enemy player))
                        enemy-color
                        (= faction (:entity/faction player))
                        friendly-color
                        :else
                        neutral-color)))))
