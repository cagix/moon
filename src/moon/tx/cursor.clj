(ns moon.tx.cursor
  (:require [gdl.graphics.cursors :as cursors]
            [moon.component :as component]))

(defmethods :tx/cursor
  (component/handle [[_ cursor-key]]
    (cursors/set cursor-key)
    nil))
