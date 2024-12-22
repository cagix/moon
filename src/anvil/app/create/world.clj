(ns anvil.app.create.world
  (:require [anvil.app.create :as create]
            [anvil.world :as world]))

(defn-impl create/world [world]
  (world/create world))
