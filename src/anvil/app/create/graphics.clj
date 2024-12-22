(ns anvil.app.create.graphics
  (:require [anvil.app.create :as create]
            [gdl.graphics :as g]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/graphics [config]
  (g/setup config))
