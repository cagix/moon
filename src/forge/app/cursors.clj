(ns forge.app.cursors
  (:require [anvil.disposable :as disposable]
            [anvil.graphics :as g]
            [clojure.utils :refer [bind-root mapvals]]))

(defn create [[_ data]]
  (bind-root g/cursors (mapvals g/cursor data)))

(defn dispose [_]
  (run! disposable/dispose (vals g/cursors)))
