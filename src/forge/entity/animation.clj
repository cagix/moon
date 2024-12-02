(ns ^:no-doc forge.entity.animation
  (:require [forge.entity :as entity]
            [forge.world :as world]))

(defprotocol Animation
  (tick [_ delta])
  (restart [_])
  (stopped? [_])
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

  (stopped? [_]
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

(defmethod edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (create (map edn->image frames)
          :frame-duration frame-duration
          :looping? looping?))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (current-frame animation)))

(defmethod entity/create :entity/animation [[_ animation] eid]
  (swap! eid assoc-image-current-frame animation))

(defmethod entity/tick :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc k (tick animation world/delta)))))

(defmethod entity/create :entity/delete-after-animation-stopped [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defmethod entity/tick :entity/delete-after-animation-stopped [_ eid]
  (when (stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))
