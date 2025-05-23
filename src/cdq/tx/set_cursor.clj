(ns cdq.tx.set-cursor
  (:require [cdq.g :as g]))

(defn do! [ctx cursor]
  (g/set-cursor! ctx cursor))
