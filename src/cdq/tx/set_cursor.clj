(ns cdq.tx.set-cursor
  (:require [gdl.c :as c]))

(defn do! [ctx cursor]
  (c/set-cursor! ctx cursor))
