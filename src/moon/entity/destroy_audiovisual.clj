(ns moon.entity.destroy-audiovisual
  (:require [moon.world.entities :as entities]))

(defn destroy [audiovisuals-id eid]
  (entities/audiovisual (:position @eid) audiovisuals-id)
  nil)
