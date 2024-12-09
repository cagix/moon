(ns forge.entity.destroy-audiovisual
  (:require [anvil.db :as db]
            [forge.world :refer [spawn-audiovisual]]))

(defn destroy [[_ audiovisuals-id] eid]
  (spawn-audiovisual (:position @eid)
                     (db/build audiovisuals-id)))
