(ns moon.tx.cursor
  (:require [moon.component :as component :refer [defc]]
            [moon.graphics.cursors :as cursors]))

(defc :tx/cursor
  (component/handle [[_ cursor-key]]
    (cursors/set cursor-key)
    nil))
