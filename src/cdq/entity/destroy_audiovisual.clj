(ns cdq.entity.destroy-audiovisual
  (:require [cdq.entity :as entity]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid _ctx]
    [[:tx/audiovisual (entity/position @eid) audiovisuals-id]]))
