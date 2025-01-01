(ns ^:no-doc anvil.entity.mouseover?
  (:require [anvil.entity :as entity]
            [clojure.component :as component :refer [defcomponent]]
            [gdl.context :as c]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defcomponent :entity/mouseover?
  (component/render-below [_
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
                          neutral-color))))))
