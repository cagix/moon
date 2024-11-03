(ns moon.entity.string-effect
  (:require [gdl.graphics.text :as text]
            [gdl.graphics.world-view :as world-view]
            [moon.world.time :as time :refer [timer stopped?]]))

(defn tick [[k {:keys [counter]}] eid]
  (when (stopped? counter)
    [[:e/dissoc eid k]]))

(defn render-above [[_ {:keys [text]}] entity]
  (let [[x y] (:position entity)]
    (text/draw {:text text
                :x x
                :y (+ y (:half-height entity) (world-view/pixels->units 5))
                :scale 2
                :up? true})))

(defn handle [[k eid text]]
  [[:e/assoc eid k
    (if-let [string-effect (k @eid)]
      (-> string-effect
          (update :text str "\n" text)
          (update :counter time/reset))
      {:text text
       :counter (timer 0.4)})]])
