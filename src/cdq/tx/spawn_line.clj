(ns cdq.tx.spawn-line
  (:require [cdq.tx.spawn-entity]))

(defn do! [{:keys [ctx/effect-body-props] :as ctx}
           {:keys [start end duration color thick?]}]
  (cdq.tx.spawn-entity/do! ctx
                           start
                           effect-body-props
                           #:entity {:line-render {:thick? thick? :end end :color color}
                                     :delete-after-duration duration}))
