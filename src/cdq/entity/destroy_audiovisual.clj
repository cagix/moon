(ns cdq.entity.destroy-audiovisual
  (:require [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id]
                    eid
                    {:keys [ctx/db]}]
    [[:tx/audiovisual
      (:position @eid)
      (db/build db audiovisuals-id)]]))
