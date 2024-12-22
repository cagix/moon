(ns anvil.app.dispose.graphics
  (:require [anvil.app.dispose :as dispose]
            [gdl.graphics :as g]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/graphics []
  (g/cleanup))
