(ns cdq.tx.add-skill
  (:require [cdq.tx :as tx]))

(defn do! [eid skill]
  (tx/add-skill eid skill))
