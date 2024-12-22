(ns anvil.app.dispose.graphics
  (:require [anvil.app.dispose :as dispose]
            [gdl.graphics :as g]))

(defn-impl dispose/graphics []
  (g/cleanup))
