(ns cdq.world.explored-tile-corners
  (:require [cdq.grid2d :as g2d]))

(defn create [width height]
  (atom (g2d/create-grid width height (constantly false))))
