(ns cdq.world)

(defprotocol World
  (cell [_ position]))
