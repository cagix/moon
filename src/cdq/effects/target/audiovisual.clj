(ns cdq.effects.target.audiovisual
  (:require [cdq.world.entity :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _effect-ctx _world]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]} _world]
  [[:tx/audiovisual (entity/position @target) audiovisual]])
