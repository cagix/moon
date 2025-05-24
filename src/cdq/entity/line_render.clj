(ns cdq.entity.line-render
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/line-render
  (entity/render-default! [[_ {:keys [thick? end color]}]
                           entity
                           _ctx]
    (let [position (entity/position entity)]
      (if thick?
        [[:draw/with-line-width 4 [[:draw/line position end color]]]]
        [[:draw/line position end color]]))))
