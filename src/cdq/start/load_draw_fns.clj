(ns cdq.start.load-draw-fns
  (:require [cdq.effects]))

(defn do! [ctx]
  (update ctx :ctx/draw-fns cdq.effects/walk-method-map))
