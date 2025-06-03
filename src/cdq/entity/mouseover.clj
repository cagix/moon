(ns cdq.entity.mouseover
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below! :entity/mouseover? [_
                                                    entity
                                                    {:keys [ctx/player-eid]}]
  (let [player @player-eid
        faction (entity/faction entity)]
    [[:draw/with-line-width 3
      [[:draw/ellipse
        (entity/position entity)
        (:half-width entity)
        (:half-height entity)
        (cond (= faction (entity/enemy player))
              enemy-color
              (= faction (entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))
