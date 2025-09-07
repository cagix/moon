(ns cdq.create.draw-fns
  (:require cdq.draw-impl))

(defn do! [ctx]
  (assoc ctx :ctx/draw-fns cdq.draw-impl/draw-fns))
