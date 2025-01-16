(ns clojure.level
  (:require [clojure.level.modules :refer [generate-modules]]
            [clojure.level.uf-caves :as uf-caves]
            [clojure.db :as db]
            [clojure.tiled :as tiled]
            [clojure.maps.tiled.tmx-map-loader :as tmx-map-loader]))

(defmulti generate-level* (fn [world c] (:world/generator world)))

(defn generate-level [c world-props]
  (assoc (generate-level* world-props c)
         :world/player-creature
         (:world/player-creature world-props)))

(defmethod generate-level* :world.generator/uf-caves [world {:keys [clojure/db] :as c}]
  (uf-caves/create world
                   (db/build-all db :properties/creatures c)
                   ((:clojure/assets c) "maps/uf_terrain.png"))) ; TODO use (def assets ::assets)

(defmethod generate-level* :world.generator/tiled-map [world c]
  {:tiled-map (tmx-map-loader/load (:world/tiled-map world))
   :start-position [32 71]})

(defmethod generate-level* :world.generator/modules [world {:keys [clojure/db] :as c}]
  (generate-modules world
                    (db/build-all db :properties/creatures c)))

(defn create [world-id {:keys [clojure/db] :as context}]
  (generate-level context (db/build db world-id context)))
