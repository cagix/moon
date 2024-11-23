(ns ^:no-doc moon.effects.target.convert
  (:require [moon.entity :as entity]))

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (entity/enemy @source))))

(defn handle [_ {:keys [effect/source effect/target]}]
  (swap! target assoc :entity/faction (:entity/faction @source)))
