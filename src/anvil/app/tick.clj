(ns anvil.app.tick
  (:require [anvil.app :as app]
            [anvil.world :as world]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/tick [_]
  (stage/act)
  (world/tick))
