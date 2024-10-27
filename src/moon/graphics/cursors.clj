(ns moon.graphics.cursors
  (:refer-clojure :exclude [set])
  (:require [gdl.graphics :as graphics]
            [gdl.utils :refer [safe-get mapvals]]))

(declare ^:private cursors)

(defn init [cursors]
  (bind-root #'cursors
             (mapvals (fn [[file hotspot]]
                        (graphics/cursor (str "cursors/" file ".png") hotspot))
                      cursors)))

(defn set [cursor-key]
  (graphics/set-cursor (safe-get cursors cursor-key)))

(defn dispose []
  (run! gdl.utils/dispose (vals cursors)))
