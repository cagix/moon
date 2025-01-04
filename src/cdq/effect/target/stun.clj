(ns cdq.effect.target.stun
  (:require [cdq.context :as world]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]} c]
  (world/send-event! c target :stun duration))
