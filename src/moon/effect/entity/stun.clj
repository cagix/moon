(ns moon.effect.entity.stun
  (:require [gdl.utils :refer [readable-number]]
            [moon.effect :refer [target]]))

(defn info [[_ duration]]
  (str "Stuns for " (readable-number duration) " seconds"))

(defn applicable? [_]
  (and target (:entity/fsm @target)))

(defn handle [[_ duration]]
  [[:entity/fsm target :stun duration]])
