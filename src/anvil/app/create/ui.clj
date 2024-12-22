(ns anvil.app.create.ui
  (:require [anvil.app.create :as create]
            [gdl.ui :as ui]
            [gdl.utils :refer [defn-impl]]))

(defn-impl create/ui [config]
  (ui/setup config))
