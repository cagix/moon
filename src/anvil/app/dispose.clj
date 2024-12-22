(ns anvil.app.dispose
  (:require [anvil.app :as app]
            [gdl.utils :refer [defn-impl]]))

(defn assets   [])
(defn graphics [])
(defn ui       [])
(defn stage    [])
(defn world    [])

(defn-impl app/dispose [_]
  (assets)
  (graphics)
  (ui)
  (stage)
  (world))
