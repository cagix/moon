(ns cdq.tx.spawn-line
  (:require [cdq.ctx :as ctx]
            [cdq.impl.world :as world]))

(defn do! [{:keys [start end duration color thick?]}]
  (world/spawn-entity! start
                       ctx/effect-body-props
                       #:entity {:line-render {:thick? thick? :end end :color color}
                                 :delete-after-duration duration}))
