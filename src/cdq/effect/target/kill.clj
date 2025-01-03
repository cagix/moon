(ns cdq.effect.target.kill
  (:require [anvil.entity :as entity]))

(defn info [_ _c]
  "Kills target")

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]} c]
  (entity/event c target :kill))
