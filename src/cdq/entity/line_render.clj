(ns cdq.entity.line-render
  (:require [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/line-render
  (entity/render-default! [[_ {:keys [thick? end color]}]
                           entity
                           draw]
    (let [position (:position entity)]
      (if thick?
        (draw/with-line-width draw 4
          #(draw/line draw position end color))
        (draw/line draw position end color)))))
