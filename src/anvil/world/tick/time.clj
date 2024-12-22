(ns anvil.world.tick.time
  (:require [anvil.world :as world]
            [anvil.world.tick :as tick]
            [gdl.graphics :as g]))

(defn-impl tick/time []
  (let [delta-ms (min (g/delta-time) world/max-delta-time)]
    (alter-var-root #'world/elapsed-time + delta-ms)
    (bind-root world/delta-time delta-ms)))
