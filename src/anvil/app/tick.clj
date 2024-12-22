(ns anvil.app.tick
  (:require [anvil.app :as app]
            [anvil.world :as world]
            [gdl.stage :as stage]))

(defn-impl app/tick [_]
  (stage/act)
  (world/tick))
