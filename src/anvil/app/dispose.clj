(ns anvil.app.dispose
  (:require [anvil.app :as app]
            [anvil.world :as world]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(defn-impl app/dispose [_]
  (assets/cleanup)
  (g/cleanup)
  (stage/cleanup)
  (ui/cleanup)
  (world/dispose))
