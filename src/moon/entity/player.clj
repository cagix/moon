(ns moon.entity.player
  (:require [moon.entity :as entity]))

(declare eid)

(defc :entity/player?
  (entity/create [_ e-id]
    (bind-root #'eid e-id)
    nil))
