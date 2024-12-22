(ns anvil.app.dispose
  (:require [anvil.app :as app]
            [anvil.lifecycle.create :refer [dispose-world]]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui :as ui]
            [gdl.utils :refer [defn-impl]]))

(defn-impl app/dispose [_]
  (assets/cleanup)
  (g/cleanup)
  (ui/cleanup)
  (stage/cleanup)
  (dispose-world))
