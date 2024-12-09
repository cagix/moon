(ns forge.effects.target.kill
  (:require [anvil.entity :refer [send-event]]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]}]
  (send-event target :kill))
