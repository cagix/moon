(ns ^:no-doc forge.effects.target.stun
  (:require [forge.entity.components :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [duration {:keys [effect/target]}]
  (entity/event target :stun duration))
