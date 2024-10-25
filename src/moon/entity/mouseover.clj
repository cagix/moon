(ns moon.entity.mouseover
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.world :as world]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defc :entity/mouseover?
  (entity/render-below [_ {:keys [entity/faction] :as entity}]
    (let [player @world/player]
      (g/with-shape-line-width 3
        #(g/draw-ellipse (:position entity)
                         (:half-width entity)
                         (:half-height entity)
                         (cond (= faction (entity/enemy player))
                               enemy-color
                               (= faction (entity/friend player))
                               friendly-color
                               :else
                               neutral-color))))))
