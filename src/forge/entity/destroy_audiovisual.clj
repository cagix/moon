(ns forge.entity.destroy-audiovisual
  (:require [anvil.db :as db]
            [anvil.entity :as entity]))

(defn destroy [[_ audiovisuals-id] eid]
  (entity/audiovisual (:position @eid)
                      (db/build audiovisuals-id)))
