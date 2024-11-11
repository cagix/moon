(ns moon.effect.entity.kill
  (:require [moon.entity.fsm :as fsm]))

(defn info [_]
  "Kills target")

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]}]
  (fsm/event target :kill))
