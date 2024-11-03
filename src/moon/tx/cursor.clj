(ns moon.tx.cursor
  (:require [gdl.graphics.cursors :as cursors]))

(defn handle [[_ cursor-key]]
  (cursors/set cursor-key)
  nil)
