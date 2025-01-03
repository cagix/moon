(ns cdq.effect.audiovisual
  (:require [cdq.context :as world]))

(defn applicable? [_ {:keys [effect/target-position]}]
  target-position)

(defn useful? [_ _ _c]
  false)

(defn handle [[_ audiovisual] {:keys [effect/target-position]} c]
  (world/audiovisual c target-position audiovisual))
