(ns forge.effects.target.audiovisual
  (:require [anvil.entity :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]}]
  (entity/audiovisual (:position @target) audiovisual))
