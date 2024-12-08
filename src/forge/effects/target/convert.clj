(ns forge.effects.target.convert
  (:require [forge.entity.faction :as faction]))

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (faction/enemy @source))))

(defn handle [_ {:keys [effect/source effect/target]}]
  (swap! target assoc :entity/faction (:entity/faction @source)))
