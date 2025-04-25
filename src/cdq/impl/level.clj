(ns cdq.impl.level
  (:require [cdq.create.level :refer [generate-level*]]
            [cdq.level.modules :refer [generate-modules]]
            [cdq.level.uf-caves :as uf-caves]
            [cdq.db :as db]
            [cdq.tiled :as tiled]))

(defmethod generate-level* :world.generator/uf-caves [world {:keys [cdq/db] :as c}]
  (uf-caves/create world
                   (db/build-all db :properties/creatures c)
                   ((:cdq/assets c) "maps/uf_terrain.png")))

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tiled/load-map (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world {:keys [cdq/db] :as c}]
  (generate-modules world
                    (db/build-all db :properties/creatures c)))
