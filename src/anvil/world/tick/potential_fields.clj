(ns anvil.world.tick.potential-fields
  (:require [anvil.lifecycle.potential-fields :refer [update-potential-fields!]]
            [anvil.world :as world]
            [anvil.world.tick :as tick]
            [gdl.utils :refer [defn-impl]]))

(defn-impl tick/potential-fields []
  (update-potential-fields! (world/active-entities)))
