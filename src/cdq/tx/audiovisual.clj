(ns cdq.tx.audiovisual
  (:require [cdq.world :as world]))

(defn do! [position audiovisual]
  (world/spawn-audiovisual position audiovisual))
