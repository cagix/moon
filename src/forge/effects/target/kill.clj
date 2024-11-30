(ns ^:no-doc forge.effects.target.kill
  (:require [moon.entity :as entity]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [_ {:keys [effect/target]}]
  (entity/event target :kill))
