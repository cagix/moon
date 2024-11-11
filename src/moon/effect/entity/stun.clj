(ns moon.effect.entity.stun
  (:require [gdl.utils :refer [readable-number]]
            [moon.entity.fsm :as fsm]))

(defn info [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [duration {:keys [effect/target]}]
  (fsm/event target :stun duration))
