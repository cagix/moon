(ns anvil.app.create.ui
  (:require [anvil.app.create :as create]
            [gdl.ui :as ui]))

(defn-impl create/ui [config]
  (ui/setup config))
