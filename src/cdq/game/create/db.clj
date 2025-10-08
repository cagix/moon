(ns cdq.game.create.db
  (:require [cdq.impl.db]))

(defn do! [ctx]
  (assoc ctx :ctx/db (cdq.impl.db/create)))
