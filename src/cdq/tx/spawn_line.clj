(ns cdq.tx.spawn-line
  (:require [cdq.g :as g]))

(defn do! [{:keys [ctx/effect-body-props] :as ctx}
           {:keys [start end duration color thick?]}]
  (g/spawn-entity! ctx
                   start
                   effect-body-props
                   #:entity {:line-render {:thick? thick? :end end :color color}
                             :delete-after-duration duration}))
