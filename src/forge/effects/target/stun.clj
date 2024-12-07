(ns forge.effects.target.stun
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]
            [forge.entity.fsm :refer [send-event]]))

(defmethods :effects.target/stun
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [[_ duration] {:keys [effect/target]}]
    (send-event target :stun duration)))
