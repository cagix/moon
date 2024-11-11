(ns moon.effect.entity.stun
  (:require [gdl.utils :refer [readable-number]]
            [moon.effect :refer [target]]
            [moon.entity.fsm :as fsm]))

(defn info [duration]
  (str "Stuns for " (readable-number duration) " seconds"))

(defn applicable? [_]
  (and target (:entity/fsm @target)))

(defn handle [duration]
  (fsm/event target :stun duration))
