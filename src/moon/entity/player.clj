(ns moon.entity.player
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.world :as world]))

(defc :entity/player?
  (entity/create [_ eid]
    (bind-root #'world/player eid)
    nil))
