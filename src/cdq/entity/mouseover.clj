(ns cdq.entity.mouseover
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethod entity/render-below! :entity/mouseover?
  [_
   {:keys [entity/faction] :as entity}
   {:keys [ctx/player-eid] :as ctx}]
  (let [player @player-eid]
    (draw/with-line-width ctx 3
      #(draw/ellipse ctx
                     (:position entity)
                     (:half-width entity)
                     (:half-height entity)
                     (cond (= faction (entity/enemy player))
                           enemy-color
                           (= faction (:entity/faction player))
                           friendly-color
                           :else
                           neutral-color)))))
