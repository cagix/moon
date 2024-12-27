(ns anvil.level
  (:require [anvil.app :as app]
            [gdl.context :as c]

            [anvil.level.modules :refer [generate-modules]]
            [anvil.level.uf-caves :as uf-caves]
            [gdl.tiled :as tiled]))

(defmulti generate-level* (fn [world] (:world/generator world)))

(defn generate-level [world-props]
  (assoc (generate-level* world-props)
         :world/player-creature
         (:world/player-creature world-props)))

(defmethod generate-level* :world.generator/uf-caves [world]
  (uf-caves/create world))

(defmethod generate-level* :world.generator/tiled-map [world]
  {:tiled-map (tiled/load-tmx-map (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world]
  (generate-modules world
                    (c/build-all @app/state :properties/creatures)))
