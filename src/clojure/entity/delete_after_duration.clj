(ns clojure.entity.delete-after-duration
  (:require [clojure.entity :as entity]
            [clojure.timer :as timer]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/delete-after-duration
  (entity/create [[_ duration] {:keys [ctx/elapsed-time]}]
    (timer/create elapsed-time duration))

  (entity/tick! [[_ counter] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/mark-destroyed eid]])))
