(ns moon.tx.cursor
  (:require [gdl.graphics.cursors :as cursors]))

(defn handle [cursor-key]
  (cursors/set cursor-key)
  nil)
