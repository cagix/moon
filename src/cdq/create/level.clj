(ns cdq.create.level
  (:require [cdq.level.modules :refer [generate-modules]]
            [cdq.level.uf-caves :as uf-caves]
            [cdq.db :as db]
            [gdl.gdx.tiled :as tiled]))

(defmulti generate-level* (fn [world c] (:world/generator world)))

(defn generate-level [c world-props]
  (assoc (generate-level* world-props c)
         :world/player-creature
         (:world/player-creature world-props)))

(defmethod generate-level* :world.generator/uf-caves [world {:keys [cdq/db] :as c}]
  (uf-caves/create world
                   (db/build-all db :properties/creatures c)
                   ((:cdq/assets c) "maps/uf_terrain.png"))) ; TODO use (def assets ::assets)

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tiled/load-map (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world {:keys [cdq/db] :as c}]
  (generate-modules world
                    (db/build-all db :properties/creatures c)))

(defn create [{:keys [cdq/db] :as context} world-id]
  (generate-level context (db/build db world-id context)))
