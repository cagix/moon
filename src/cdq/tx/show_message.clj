(ns cdq.tx.show-message
  (:require [cdq.g :as g]
            [cdq.ui.message :as message]))

(defn do! [ctx message]
  (message/show! (g/find-actor-by-name ctx "player-message")
                 message))
