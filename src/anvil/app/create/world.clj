(ns anvil.app.create.world
  (:require [anvil.app.create :as create]
            [anvil.world :as world]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/world [world]
  (world/create world))
