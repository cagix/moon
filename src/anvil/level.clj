(ns anvil.level
  (:require [anvil.level.modules :refer [generate-modules]]
            [anvil.level.uf-caves :as uf-caves]
            [gdl.context :as c]
            [gdl.tiled :as tiled]))

(defmulti generate-level* (fn [world c] (:world/generator world)))

(defn generate-level [c world-props]
  (assoc (generate-level* world-props c)
         :world/player-creature
         (:world/player-creature world-props)))

(defmethod generate-level* :world.generator/uf-caves [world c]
  (uf-caves/create world
                   (c/build-all c :properties/creatures)
                   ((:gdl.context/assets c) "maps/uf_terrain.png")))

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tiled/load-tmx-map (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world c]
  (generate-modules world
                    (c/build-all c :properties/creatures)))
