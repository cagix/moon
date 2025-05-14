(ns cdq.tx.spawn-projectile
  (:require [cdq.world :as world]))

(defn do! [opts projectile]
  (world/spawn-projectile opts projectile))
