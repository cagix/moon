(ns moon.entity.player
  (:require [moon.component :refer [defc]]
            [moon.entity :as entity]
            [moon.world :as world]))

(defc :entity/player?
  (entity/create [_ eid]
    (.bindRoot #'world/player eid)
    nil))
