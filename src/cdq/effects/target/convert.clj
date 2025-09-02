(ns cdq.effects.target.convert
  (:require [cdq.world.entity.faction :as faction]))

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (:entity/faction @target)
          (faction/enemy (:entity/faction @source)))))

(defn handle [_ {:keys [effect/source effect/target]} _world]
  [[:tx/assoc target :entity/faction (:entity/faction @source)]])
