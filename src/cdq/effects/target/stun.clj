(ns cdq.effects.target.stun
  (:require [clojure.utils :as utils]))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]} _world]
  [[:tx/event target :stun duration]])
