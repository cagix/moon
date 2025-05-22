(ns cdq.tx.spawn-line
  (:require [cdq.g :as g]))

(defn do! [ctx
           {:keys [start end duration color thick?]}]
  (g/spawn-effect! ctx
                   start
                   #:entity {:line-render {:thick? thick? :end end :color color}
                             :delete-after-duration duration}))
