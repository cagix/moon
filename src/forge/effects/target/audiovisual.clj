(ns forge.effects.target.audiovisual
  (:require [anvil.world :refer [spawn-audiovisual]]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]}]
  (spawn-audiovisual (:position @target) audiovisual))
