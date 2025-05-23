(ns cdq.tx.show-message
  (:require [cdq.c :as c]
            [cdq.ui.message :as message]))

(defn do! [ctx message]
  (message/show! (c/find-actor-by-name ctx "player-message")
                 message))
