(ns moon.effect.entity.kill
  (:require [moon.effect :refer [target]]))

(defn info [_]
  "Kills target")

(defn applicable? [_]
  (and target (:entity/fsm @target)))

(defn handle [_]
  [[:entity/fsm target :kill]])
