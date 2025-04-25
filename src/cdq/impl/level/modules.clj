(ns cdq.impl.level.modules
  (:require [cdq.create.level :refer [generate-level*]]
            [cdq.level.modules :refer [generate-modules]]
            [cdq.db :as db]))

(defmethod generate-level* :world.generator/modules [world {:keys [cdq/db] :as c}]
  (generate-modules world
                    (db/build-all db :properties/creatures c)))
