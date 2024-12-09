(ns forge.entity.string-effect
  (:require [anvil.graphics :as g]
            [anvil.world :refer [timer reset-timer stopped?]]))

(defn add [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn tick [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    (swap! eid dissoc k)))

(defn render-above [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (g/draw-text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (g/pixels->world-units 5))
                  :scale 2
                  :up? true})))
