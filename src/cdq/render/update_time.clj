(ns cdq.render.update-time
  (:require [cdq.w :as w]
            [cdq.graphics :as g]))

(defn do! [{:keys [ctx/graphics
                   ctx/world]
            :as ctx}]
  (update ctx :ctx/world w/update-time (g/delta-time graphics)))
