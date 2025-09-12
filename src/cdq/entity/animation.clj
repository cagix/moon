(ns cdq.entity.animation
  (:require [cdq.entity.image :as image]))

(defprotocol PAnimation
  (tick [_ delta])
  (restart [_])
  (stopped? [_])
  (current-frame [_]))

(defrecord RAnimation [frames frame-duration looping? cnt maxcnt]
  PAnimation
  (tick [this delta]
    (let [maxcnt (float maxcnt)
          newcnt (+ (float cnt) (float delta))]
      (assoc this :cnt (cond (< newcnt maxcnt) newcnt
                             looping? (min maxcnt (- newcnt maxcnt))
                             :else maxcnt))))

  (restart [this]
    (assoc this :cnt 0))

  (stopped? [_]
    (and (not looping?) (>= cnt maxcnt)))

  (current-frame [this]
    (frames (min (int (/ (float cnt) (float frame-duration)))
                 (dec (count frames))))))

(defn- create-animation
  [{:keys [animation/frames
           animation/frame-duration
           animation/looping?]}]
  (map->RAnimation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))}))

(defn create [v _ctx]
  (create-animation v))

(defn tick! [animation eid {:keys [ctx/delta-time]}]
  [[:tx/assoc eid :entity/animation (tick animation delta-time)]])

(defn draw [animation entity ctx]
  (image/draw (current-frame animation)
              entity
              ctx))
