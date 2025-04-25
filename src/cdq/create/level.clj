(ns cdq.create.level
  (:require [cdq.db :as db]))

(defmulti generate-level* (fn [world c] (:world/generator world)))

(defn generate-level [c world-props]
  (assoc (generate-level* world-props c)
         :world/player-creature
         (:world/player-creature world-props)))

(defn create [{:keys [cdq/db] :as context} world-id]
  (generate-level context (db/build db world-id context)))
