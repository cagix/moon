(ns cdq.render.draw-tiled-map
  (:require [cdq.g :as g]))

(defn do! [ctx]
  (g/draw-world-map! ctx))
