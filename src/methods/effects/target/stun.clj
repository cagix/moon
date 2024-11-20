(ns methods.effects.target.stun
  (:require [gdl.utils :refer [readable-number]]
            [moon.entity :as entity]))

(defn info [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [duration {:keys [effect/target]}]
  (entity/event target :stun duration))
