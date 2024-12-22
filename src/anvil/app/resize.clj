(ns anvil.app.resize
  (:require [anvil.app :as app]
            [gdl.graphics :as g]))

(defn-impl app/resize [_ w h]
  (g/resize w h))
