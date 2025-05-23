(ns cdq.tx.show-message
  (:require [cdq.ui.message :as message]
            [gdl.c :as c]))

(defn do! [ctx message]
  (message/show! (c/find-actor-by-name ctx "player-message")
                 message))
