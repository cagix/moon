(ns ^:no-doc moon.effects.target.stun
  (:require [moon.entity :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [duration {:keys [effect/target]}]
  (entity/event target :stun duration))
