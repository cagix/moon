(ns cdq.create.gdl
  (:require [gdl.context]))

(defn do! [ctx config]
  (merge ctx
         (gdl.context/create config)))
