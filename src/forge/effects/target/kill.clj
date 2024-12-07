(ns forge.effects.target.kill
  (:require [clojure.utils :refer [defmethods]]
            [forge.effect :refer [applicable? handle]]
            [forge.entity.fsm :refer [send-event]]))

(defmethods :effects.target/kill
  (applicable? [_ {:keys [effect/target]}]
    (and target
         (:entity/fsm @target)))

  (handle [_ {:keys [effect/target]}]
    (send-event target :kill)))
