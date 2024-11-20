(ns methods.effects.target.kill
  (:require [moon.entity :as entity]))

(defn info [_]
  "Kills target")

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]}]
  (entity/event target :kill))
