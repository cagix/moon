(ns forge.world.player
  (:require [forge.utils :refer [bind-root]]))

(declare player-eid)

(defn init [eid]
  (bind-root player-eid eid))
