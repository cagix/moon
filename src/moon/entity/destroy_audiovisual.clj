(ns ^:no-doc moon.entity.destroy-audiovisual
  (:require [moon.db :as db]
            [moon.world :as world]))

(defn destroy [audiovisuals-id eid]
  (world/audiovisual (:position @eid) (db/get audiovisuals-id)))
