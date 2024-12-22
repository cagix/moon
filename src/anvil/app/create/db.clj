(ns anvil.app.create.db
  (:require [anvil.app.create :as create]
            [anvil.db]
            [gdl.db]))

(defn-impl create/db [config]
  (bind-root anvil.db/db (gdl.db/create config)))
