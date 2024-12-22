(ns anvil.app.dispose.world
  (:require [anvil.app.dispose :as dispose]
            [anvil.world :as world]))

(defn-impl dispose/world []
  (world/dispose))
