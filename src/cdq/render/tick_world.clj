(ns cdq.render.tick-world
  (:require cdq.core))

(defn do! [ctx render-fns]
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (reduce cdq.core/render* ctx render-fns)))
