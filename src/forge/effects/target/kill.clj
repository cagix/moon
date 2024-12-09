(ns forge.effects.target.kill
  (:require [anvil.fsm :as fsm]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]}]
  (fsm/event target :kill))
