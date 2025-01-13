(ns clojure.level
  (:require [clojure.level.modules :refer [generate-modules]]
            [clojure.level.uf-caves :as uf-caves]
            [clojure.context :as c]
            [clojure.tiled :as tiled]
            [clojure.maps.tiled.tmx-map-loader :as tmx-map-loader]))

(defmulti generate-level* (fn [world c] (:world/generator world)))

(defn generate-level [c world-props]
  (assoc (generate-level* world-props c)
         :world/player-creature
         (:world/player-creature world-props)))

(defmethod generate-level* :world.generator/uf-caves [world c]
  (uf-caves/create world
                   (c/build-all c :properties/creatures)
                   ((:clojure/assets c) "maps/uf_terrain.png"))) ; TODO use (def assets ::assets)

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tmx-map-loader/load (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world c]
  (generate-modules world
                    (c/build-all c :properties/creatures)))

(defn create [world-id context]
  (generate-level context (c/build context world-id)))
