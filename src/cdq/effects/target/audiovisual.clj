(ns cdq.effects.target.audiovisual
  (:require [cdq.entity :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _effect-ctx _ctx]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]} _ctx]
  [[:tx/audiovisual (entity/position @target) audiovisual]])
