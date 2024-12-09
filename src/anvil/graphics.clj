(ns anvil.graphics
  (:require [clojure.gdx.graphics :as g]
            [clojure.utils :refer [safe-get]]))

(declare cursors)

(defn set-cursor [cursor-key]
  (g/set-cursor (safe-get cursors cursor-key)))
