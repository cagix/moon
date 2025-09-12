(ns cdq.effects.target.convert
  (:require [cdq.entity.faction :as faction]))

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (faction/enemy (:entity/faction @source)))))

(defn handle [_ {:keys [effect/source effect/target]} _ctx]
  [[:tx/assoc target :entity/faction (:entity/faction @source)]])

(defn info-text [_ _ctx]
  "Converts target to your side.")
