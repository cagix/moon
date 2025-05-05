(ns cdq.level
  (:require [cdq.db :as db]))

(defmulti generate-level* :world/generator)

(defn generate-level [world-props]
  (assoc (generate-level* world-props)
         :world/player-creature
         (:world/player-creature world-props)))

(defn create [world-id]
  (generate-level (db/build world-id)))
