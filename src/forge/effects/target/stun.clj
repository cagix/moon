(ns forge.effects.target.stun
  (:require [anvil.fsm :as fsm]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]}]
  (fsm/event target :stun duration))
