(ns forge.entity.string-effect
  (:require [anvil.graphics :refer [draw-text]]
            [forge.app.world-viewport :refer [pixels->world-units]]
            [forge.world.time :refer [timer reset-timer stopped?]]))

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
    (draw-text {:text text
                :x x
                :y (+ y
                      (:half-height entity)
                      (pixels->world-units 5))
                :scale 2
                :up? true})))
