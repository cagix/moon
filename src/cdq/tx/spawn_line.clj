(ns cdq.tx.spawn-line
  (:require [cdq.ctx :as ctx]
            [cdq.tx.spawn-entity]))

(defn do! [{:keys [start end duration color thick?]}]
  (cdq.tx.spawn-entity/do! start
                           ctx/effect-body-props
                           #:entity {:line-render {:thick? thick? :end end :color color}
                                     :delete-after-duration duration}))
