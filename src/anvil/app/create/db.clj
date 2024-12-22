(ns anvil.app.create.db
  (:require [anvil.app.create :as create]
            [anvil.db]
            [gdl.db]
            [gdl.utils :refer [defn-impl bind-root]]))

(defn-impl create/db [config]
  (bind-root anvil.db/db (gdl.db/create config)))
