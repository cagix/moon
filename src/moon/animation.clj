(ns moon.animation)

(defprotocol Animation
  (tick [_ delta])
  (restart [_])
  (anim-stopped? [_])
  (current-frame [_]))

(defrecord ImmutableAnimation [frames frame-duration looping? cnt maxcnt]
  Animation
  (tick [this delta]
    (let [maxcnt (float maxcnt)
          newcnt (+ (float cnt) (float delta))]
      (assoc this :cnt (cond (< newcnt maxcnt) newcnt
                             looping? (min maxcnt (- newcnt maxcnt))
                             :else maxcnt))))

  (restart [this]
    (assoc this :cnt 0))

  (anim-stopped? [_]
    (and (not looping?) (>= cnt maxcnt)))

  (current-frame [this]
    (frames (min (int (/ (float cnt) (float frame-duration)))
                 (dec (count frames))))))

(defn create [frames & {:keys [frame-duration looping?]}]
  (map->ImmutableAnimation
    {:frames (vec frames)
     :frame-duration frame-duration
     :looping? looping?
     :cnt 0
     :maxcnt (* (count frames) (float frame-duration))}))
