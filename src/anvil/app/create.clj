(ns anvil.app.create
  (:require [anvil.app :as app]
            [anvil.db]
            [anvil.world :as world]
            [gdl.assets :as assets]
            [gdl.db]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(defn-impl app/create [config]
  (bind-root anvil.db/db (gdl.db/create (:db config)))
  (assets/setup)
  (g/setup (:graphics config))
  (ui/setup  (:ui config))
  (stage/setup)
  (world/create (:world config)))
