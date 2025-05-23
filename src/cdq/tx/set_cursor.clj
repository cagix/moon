(ns cdq.tx.set-cursor
  (:require [cdq.c :as c]))

(defn do! [ctx cursor]
  (c/set-cursor! ctx cursor))
