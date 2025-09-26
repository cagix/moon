(ns cdq.entity.animation
  (:require [cdq.entity.image :as image]))

(defprotocol Animation
  (tick* [_ delta])
  (restart [_])
  (stopped? [_])
  (current-frame [_]))

(defrecord RAnimation
  [frames frame-duration looping? cnt maxcnt delete-after-stopped?]
  Animation
  (tick* [this delta]
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

(defn create
  [{:keys [animation/frames
           animation/frame-duration
           animation/looping?
           delete-after-stopped?]}
   _world]
  (assert (not (and looping? delete-after-stopped?)))
  (map->RAnimation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))
    :delete-after-stopped? delete-after-stopped?}))

(defn tick [animation eid {:keys [world/delta-time]}]
  [[:tx/assoc eid :entity/animation (tick* animation delta-time)]
   (when (and (:delete-after-stopped? animation)
              (stopped? animation))
     [:tx/mark-destroyed eid])])

(defn draw [animation entity ctx]
  (image/draw (current-frame animation) entity ctx))
