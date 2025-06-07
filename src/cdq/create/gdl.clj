(ns cdq.create.gdl
  (:require [gdl.context]))

(defn do! [ctx]
  (merge ctx
         (gdl.context/create (:gdl.application/context (:ctx/config ctx)))))
