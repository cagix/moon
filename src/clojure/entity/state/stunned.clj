(ns clojure.entity.state.stunned
  (:require [clojure.entity :as entity]
            [clojure.state :as state]
            [clojure.timer :as timer]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :stunned
  (entity/create [[_ _eid duration] {:keys [ctx/elapsed-time]}]
    {:counter (timer/create elapsed-time duration)})

  (entity/tick! [[_ {:keys [counter]}] eid {:keys [ctx/elapsed-time]}]
    (when (timer/stopped? elapsed-time counter)
      [[:tx/event eid :effect-wears-off]]))

  (state/cursor [_] :cursors/denied)

  (state/pause-game? [_] false)

  (entity/render-below! [_ entity _ctx]
    [[:draw/circle (entity/position entity) 0.5 [1 1 1 0.6]]]))
