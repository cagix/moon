(ns anvil.app.create.world
  (:require [anvil.app.create :as create]
            [anvil.db :as db]
            [anvil.lifecycle.create :refer [create-world]]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/world [world-id]
  (create-world (db/build world-id)))
