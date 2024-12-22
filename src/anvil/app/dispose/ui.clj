(ns anvil.app.dispose.ui
  (:require [anvil.app.dispose :as dispose]
            [gdl.ui :as ui]))

(defn-impl dispose/ui []
  (ui/cleanup))

