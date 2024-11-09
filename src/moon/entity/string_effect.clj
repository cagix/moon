(ns moon.entity.string-effect
  (:require [gdl.graphics.text :as text]
            [gdl.graphics.world-view :as world-view]
            [gdl.system :refer [*k*]]
            [moon.world.time :as time :refer [timer stopped?]]))

(defn tick [{:keys [counter]} eid]
  (when (stopped? counter)
    (swap! eid dissoc *k*)
    nil))

(defn render-above [{:keys [text]} entity]
  (let [[x y] (:position entity)]
    (text/draw {:text text
                :x x
                :y (+ y (:half-height entity) (world-view/pixels->units 5))
                :scale 2
                :up? true})))

(defn handle [eid text]
  (swap! eid assoc *k* (if-let [string-effect (*k* @eid)]
                         (-> string-effect
                             (update :text str "\n" text)
                             (update :counter time/reset))
                         {:text text
                          :counter (timer 0.4)}))
  nil)
