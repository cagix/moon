(ns moon.entity.destroy-audiovisual
  (:require [moon.db :as db]
            [moon.world.entities :as entities]))

(defn destroy [audiovisuals-id eid]
  (entities/audiovisual (:position @eid) (db/get audiovisuals-id)))
