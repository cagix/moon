(ns ^:no-doc forge.entity.destroy-audiovisual
  (:require [forge.db :as db]
            [moon.world :as world]))

(defn destroy [audiovisuals-id eid]
  (world/audiovisual (:position @eid) (db/build audiovisuals-id)))
