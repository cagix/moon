(ns cdq.effect.target.convert
  (:require [cdq.entity :as entity]))

(defn info [_ _entity _c]
  "Converts target to your side.")

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (entity/enemy @source))))

(defn handle [_ {:keys [effect/source effect/target]} c]
  (swap! target assoc :entity/faction (:entity/faction @source)))
