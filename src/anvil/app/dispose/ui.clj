(ns anvil.app.dispose.ui
  (:require [anvil.app.dispose :as dispose]
            [gdl.ui :as ui]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/ui []
  (ui/cleanup))

