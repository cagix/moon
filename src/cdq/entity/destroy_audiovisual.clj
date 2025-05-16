(ns cdq.entity.destroy-audiovisual
  (:require [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid]
    [[:tx/audiovisual (:position @eid) (db/build ctx/db audiovisuals-id)]]))
