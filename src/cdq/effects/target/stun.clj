(ns cdq.effects.target.stun
  (:require [cdq.utils :as utils]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]} _ctx]
  [[:tx/event target :stun duration]])

(defn info-text [[_ duration] _ctx]
  (str "Stuns for " (utils/readable-number duration) " seconds"))
