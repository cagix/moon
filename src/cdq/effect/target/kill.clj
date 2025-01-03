(ns cdq.effect.target.kill
  (:require [cdq.context :as world]))

(defn info [_ _c]
  "Kills target")

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]} c]
  (world/send-event! c target :kill))
