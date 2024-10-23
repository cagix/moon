(ns moon.level
  (:require [moon.db :as db]))

(defmulti generate* (fn [world] (:world/generator world)))

(defn generate [world-id]
  (let [prop (db/get world-id)]
    (assoc (generate* prop)
           :world/player-creature
           (:world/player-creature prop))))
