(ns cdq.effects.target.convert
  (:require [cdq.world.entity :as entity]))

(defn applicable? [_ {:keys [effect/source effect/target]}]
  (and target
       (= (entity/faction @target)
          (entity/enemy @source))))

(defn handle [_ {:keys [effect/source effect/target]} _world]
  [[:tx/assoc target :entity/faction (entity/faction @source)]])
