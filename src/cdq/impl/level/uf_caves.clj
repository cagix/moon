(ns cdq.impl.level.uf-caves
  (:require [cdq.create.level :refer [generate-level*]]
            [cdq.db :as db]
            [cdq.level.uf-caves :as uf-caves]))

(defmethod generate-level* :world.generator/uf-caves [world {:keys [cdq/db] :as c}]
  (uf-caves/create world
                   (db/build-all db :properties/creatures c)
                   ((:cdq/assets c) "maps/uf_terrain.png")))

