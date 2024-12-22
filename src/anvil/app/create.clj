(ns anvil.app.create
  (:require [anvil.app :as app]
            [anvil.db :as db]
            [anvil.lifecycle.create :refer [create-world]]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/create [{:keys [db graphics ui world-id]}]
  (db/setup db)
  (assets/setup)
  (g/setup graphics)
  (ui/setup ui)
  (stage/setup)
  (create-world (db/build world-id)))
