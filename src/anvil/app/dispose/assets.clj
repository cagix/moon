(ns anvil.app.dispose.assets
  (:require [anvil.app.dispose :as dispose]
            [gdl.assets :as assets]
            [gdl.utils :refer [defn-impl]]))

(defn-impl dispose/assets []
  (assets/cleanup))
