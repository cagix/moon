(ns anvil.app.create.graphics
  (:require [anvil.app.create :as create]
            [gdl.graphics :as g]))

(defn-impl create/graphics [config]
  (g/setup config))
