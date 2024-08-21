(ns world.time
  (:require [gdx.graphics :as graphics]
            [api.context :as ctx]))

; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
(def max-delta-time 0.04)

(defn ->build []
  {:context.game/elapsed-time 0
   :context.game/logic-frame 0})

(defrecord Counter [duration stop-time])

(extend-type api.context.Context
  api.context/Time
  (delta-time     [ctx] (:context.game/delta-time     ctx))
  (elapsed-time   [ctx] (:context.game/elapsed-time   ctx))
  (logic-frame    [ctx] (:context.game/logic-frame    ctx))

  (->counter [ctx duration]
    {:pre [(>= duration 0)]}
    (->Counter duration (+ (ctx/elapsed-time ctx) duration)))

  (stopped? [ctx {:keys [stop-time]}]
    (>= (ctx/elapsed-time ctx) stop-time))

  (reset [ctx {:keys [duration] :as counter}]
    (assoc counter :stop-time (+ (ctx/elapsed-time ctx) duration)))

  (finished-ratio [ctx {:keys [duration stop-time] :as counter}]
    {:post [(<= 0 % 1)]}
    (if (ctx/stopped? ctx counter)
      0
      ; min 1 because floating point math inaccuracies
      (min 1 (/ (- stop-time (ctx/elapsed-time ctx)) duration)))) )

(defn update-time [ctx]
  (let [delta (min (graphics/delta-time) max-delta-time)]
    (-> ctx
        (assoc :context.game/delta-time delta)
        (update :context.game/elapsed-time + delta)
        (update :context.game/logic-frame inc))))