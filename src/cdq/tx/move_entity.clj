(ns cdq.tx.move-entity
  (:require [cdq.g :as g]))

(defn do! [ctx & params]
  (apply g/move-entity! ctx params))
