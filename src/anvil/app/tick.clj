(ns anvil.app.tick
  (:require [anvil.app :as app]
            [gdl.stage :as stage]
            [gdl.utils :refer [defn-impl]]))

(defn world [])

(defn-impl app/tick [_]
  (stage/act)
  (world))
