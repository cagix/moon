(ns anvil.app.dispose.world
  (:require [anvil.app.dispose :as dispose]
            [anvil.lifecycle.create :refer [dispose-world]]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/world []
  (dispose-world))
