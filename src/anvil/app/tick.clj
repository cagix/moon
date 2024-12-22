(ns anvil.app.tick
  (:require [anvil.app :as app]
            [anvil.lifecycle.update :refer [update-world]]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/tick [_]
  (stage/act)
  (update-world))
