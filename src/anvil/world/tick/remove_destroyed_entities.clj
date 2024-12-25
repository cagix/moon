(ns anvil.world.tick.remove-destroyed-entities
  (:require [cdq.context :as world]
            [anvil.world.tick :as tick]))

(defn-impl tick/remove-destroyed-entities [c]
  (world/remove-destroyed-entities c))
