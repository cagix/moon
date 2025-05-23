(ns cdq.entity.string-effect
  (:require [cdq.entity :as entity]
            [cdq.c :as c]
            [cdq.g :as g]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/string-effect
  (entity/tick! [[k {:keys [counter]}] eid ctx]
    (when (g/timer-stopped? ctx counter)
      [[:tx/dissoc eid k]]))

  (entity/render-above! [[_ {:keys [text]}]
                         entity
                         ctx]
    (let [[x y] (:position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y
                          (:half-height entity)
                          (c/pixels->world-units ctx 5))
                    :scale 2
                    :up? true}]])))
