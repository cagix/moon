(ns anvil.app.create.assets
  (:require [anvil.app.create :as create]
            [gdl.assets :as assets]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/assets [_]
  (assets/setup))
