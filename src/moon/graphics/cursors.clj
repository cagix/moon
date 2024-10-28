(ns moon.graphics.cursors
  (:refer-clojure :exclude [set])
  (:require [gdl.graphics :as graphics]
            [gdl.utils :refer [dispose safe-get mapvals]]
            [moon.app :as app]
            [moon.component :refer [defc]]))

(declare ^:private cursors)

(defc :moon.graphics.cursors
  (app/create [[_ cursors]]
    (bind-root #'cursors
               (mapvals (fn [[file hotspot]]
                          (graphics/cursor (str "cursors/" file ".png") hotspot))
                        cursors)))

  (app/dispose [_]
    (run! dispose (vals cursors))))

(defn set [cursor-key]
  (graphics/set-cursor (safe-get cursors cursor-key)))
