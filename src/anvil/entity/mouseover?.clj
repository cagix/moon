(ns ^:no-doc anvil.entity.mouseover?
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.world :as world]
            [gdl.graphics :as g]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defmethods :entity/mouseover?
  (component/render-below [_ {:keys [entity/faction] :as entity}]
    (let [player @world/player-eid]
      (g/with-line-width 3
        #(g/ellipse (:position entity)
                    (:half-width entity)
                    (:half-height entity)
                    (cond (= faction (entity/enemy player))
                          enemy-color
                          (= faction (:entity/faction player))
                          friendly-color
                          :else
                          neutral-color))))))
