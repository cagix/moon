(ns moon.entity.string-effect
  (:require [moon.system :refer [*k*]]
            [moon.core :refer [draw-text pixels->world-units]]
            [moon.world.time :as time :refer [timer stopped?]]))

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)))

(defn render-above [{:keys [text]} entity]
  (let [[x y] (:position entity)]
    (draw-text {:text text
                :x x
                :y (+ y (:half-height entity) (pixels->world-units 5))
                :scale 2
                :up? true})))

(defn add [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (*k* entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter time/reset))
           {:text text
            :counter (timer 0.4)})))
