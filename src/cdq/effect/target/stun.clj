(ns cdq.effect.target.stun
  (:require [cdq.entity :as entity]
            [clojure.utils :refer [readable-number]]))

(defn info [duration _c]
  (str "Stuns for " (readable-number duration) " seconds"))

(defn applicable? [_ {:keys [effect/target]}]
  (and target
       (:entity/fsm @target)))

(defn handle [[_ duration] {:keys [effect/target]} c]
  (entity/event c target :stun duration))
