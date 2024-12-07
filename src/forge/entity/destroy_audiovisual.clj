(ns forge.entity.destroy-audiovisual
  (:require [forge.app.db :as db]
            [forge.entity :refer [destroy]]
            [forge.world :refer [spawn-audiovisual]]))

(defmethod destroy :entity/destroy-audiovisual [[_ audiovisuals-id] eid]
  (spawn-audiovisual (:position @eid)
                     (db/build audiovisuals-id)))
