(ns moon.effects.entity.audiovisual
  (:require [moon.world.entities :as entities]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _]
  false)

(defn handle [audiovisual {:keys [effect/target]}]
  (entities/audiovisual (:position @target) audiovisual))
