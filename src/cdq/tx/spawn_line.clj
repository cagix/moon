(ns cdq.tx.spawn-line
  (:require [cdq.ctx :as ctx]
            [cdq.world :as world]))

(defn do! [{:keys [start end duration color thick?]}]
  (world/spawn-entity! ctx/world
                       start
                       world/effect-body-props
                       #:entity {:line-render {:thick? thick? :end end :color color}
                                 :delete-after-duration duration}))
