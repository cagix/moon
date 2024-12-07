(ns forge.entity.string-effect
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.world-viewport :refer [pixels->world-units]]
            [forge.entity :refer [tick render-above]]
            [forge.graphics :refer [draw-text]]
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

(defmethods :entity/string-effect
  (tick [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      (swap! eid dissoc k)))

  (render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (draw-text {:text text
                  :x x
                  :y (+ y
                        (:half-height entity)
                        (pixels->world-units 5))
                  :scale 2
                  :up? true}))))
