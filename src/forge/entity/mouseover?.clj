(ns forge.entity.mouseover?
  (:require [forge.app.shape-drawer :as sd]
            [forge.entity.faction :as faction]
            [forge.world.player :refer [player-eid]]))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defn render-below [_ {:keys [entity/faction] :as entity}]
  (let [player @player-eid]
    (sd/with-line-width 3
      #(sd/ellipse (:position entity)
                   (:half-width entity)
                   (:half-height entity)
                   (cond (= faction (faction/enemy player))
                         enemy-color
                         (= faction (:entity/faction player))
                         friendly-color
                         :else
                         neutral-color)))))
