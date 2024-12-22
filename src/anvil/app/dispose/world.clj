(ns anvil.app.dispose.world
  (:require [anvil.app.dispose :as dispose]
            [anvil.world :as world]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/world []
  (world/dispose))
