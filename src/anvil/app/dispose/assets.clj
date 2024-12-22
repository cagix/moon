(ns anvil.app.dispose.assets
  (:require [anvil.app.dispose :as dispose]
            [gdl.assets :as assets]))

(defn-impl dispose/assets []
  (assets/cleanup))
