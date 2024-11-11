(ns moon.effect.entity.kill
  (:require [moon.effect :refer [target]]
            [moon.entity.fsm :as fsm]))

(defn info [_]
  "Kills target")

(defn applicable? [_]
  (and target (:entity/fsm @target)))

(defn handle [_]
  (fsm/event target :kill))
