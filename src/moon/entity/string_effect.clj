(ns moon.entity.string-effect
  (:require [moon.component :refer [defc] :as component]
            [moon.graphics :as g]
            [moon.graphics.world-view :as world-view]
            [moon.entity :as entity]
            [moon.world.time :as time :refer [timer stopped?]]))

(defc :entity/string-effect
  (entity/tick [[k {:keys [counter]}] eid]
    (when (stopped? counter)
      [[:e/dissoc eid k]]))

  (entity/render-above [[_ {:keys [text]}] entity]
    (let [[x y] (:position entity)]
      (g/draw-text {:text text
                    :x x
                    :y (+ y (:half-height entity) (world-view/pixels->units 5))
                    :scale 2
                    :up? true}))))

(defc :tx/add-text-effect
  (component/handle [[_ eid text]]
    [[:e/assoc
      eid
      :entity/string-effect
      (if-let [string-effect (:entity/string-effect @eid)]
        (-> string-effect
            (update :text str "\n" text)
            (update :counter time/reset))
        {:text text
         :counter (timer 0.4)})]]))
