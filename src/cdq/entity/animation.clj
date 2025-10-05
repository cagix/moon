(ns cdq.entity.animation
  (:require [clojure.animation]))

(defrecord Animation
  [frames frame-duration looping? cnt maxcnt delete-after-stopped?]
  clojure.animation/Animation
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

(defn create
  [{:keys [animation/frames
           animation/frame-duration
           animation/looping?
           delete-after-stopped?]}
   _world]
  (assert (not (and looping? delete-after-stopped?)))
  (map->Animation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))
    :delete-after-stopped? delete-after-stopped?}))
