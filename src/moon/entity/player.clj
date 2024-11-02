(ns moon.entity.player
  (:require [moon.entity :as entity]
            [moon.player :as player]))

(defmethods :entity/player?
  (entity/create [_ eid]
    (bind-root #'player/eid eid)
    nil))
