(ns ^:no-doc forge.effects.target.audiovisual
  (:require [forge.world :as world]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _]
  false)

(defn handle [audiovisual {:keys [effect/target]}]
  (world/audiovisual (:position @target) audiovisual))
