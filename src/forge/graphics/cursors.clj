(ns forge.graphics.cursors
  (:refer-clojure :exclude [set])
  (:require [clojure.gdx :as gdx]))

(declare cursors)

(defn set [cursor-key]
  (gdx/set-cursor (safe-get cursors cursor-key)))
