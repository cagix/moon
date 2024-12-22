(ns anvil.world.tick.remove-destroyed-entities
  (:require [anvil.world :as world]
            [anvil.world.tick :as tick]
            [gdl.utils :refer [defn-impl]]))

(defn-impl tick/remove-destroyed-entities []
  (world/remove-destroyed-entities))
