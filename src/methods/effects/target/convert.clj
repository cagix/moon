(ns methods.effects.target.convert
  (:require [moon.entity.faction :as faction]))

(defn info [_]
  "Converts target to your side.")

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (faction/enemy @source))))

(defn handle [_ {:keys [effect/source effect/target]}]
  (swap! target assoc :entity/faction (:entity/faction @source)))
