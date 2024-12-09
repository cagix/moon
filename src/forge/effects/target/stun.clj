(ns forge.effects.target.stun
  (:require [anvil.entity :refer [send-event]]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]}]
  (send-event target :stun duration))
