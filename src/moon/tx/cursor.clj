(ns moon.tx.cursor
  (:require [moon.component :as component]
            [moon.graphics.cursors :as cursors]))

(defmethods :tx/cursor
  (component/handle [[_ cursor-key]]
    (cursors/set cursor-key)
    nil))
