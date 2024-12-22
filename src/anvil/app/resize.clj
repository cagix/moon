(ns anvil.app.resize
  (:require [anvil.app :as app]
            [gdl.graphics :as g]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/resize [_ w h]
  (g/resize w h))
