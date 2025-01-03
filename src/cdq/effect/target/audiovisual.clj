(ns cdq.effect.target.audiovisual
  (:require [cdq.context :as world]))

(defn applicable? [_ {:keys [effect/target]}]
  target)

(defn useful? [_ _ _c]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target]} c]
  (world/audiovisual c
                     (:position @target)
                     audiovisual))
