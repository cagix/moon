(ns cdq.entity.destroy-audiovisual
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid ctx]
    [[:tx/audiovisual
      (:position @eid)
      (g/build ctx audiovisuals-id)]]))
