(ns moon.entity.player?
  (:require [moon.player :as player]))

(defn create [_ eid]
  (bind-root #'player/eid eid))
