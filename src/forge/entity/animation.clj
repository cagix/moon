(ns ^:no-doc forge.entity.animation
  (:require [forge.db :as db]
            [forge.editor.widget :as widget]
            [forge.entity :as entity]
            [forge.schema :as schema]
            [forge.schema.image :as image]
            [forge.ui :as ui]
            [moon.world :as world]))

(defprotocol Animation
  (anim-tick [_ delta])
  (restart [_])
  (stopped? [_])
  (current-frame [_]))

(defrecord ImmutableAnimation [frames frame-duration looping? cnt maxcnt]
  Animation
  (anim-tick [this delta]
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

(defn- create [frames & {:keys [frame-duration looping?]}]
  (map->ImmutableAnimation
    {:frames (vec frames)
     :frame-duration frame-duration
     :looping? looping?
     :cnt 0
     :maxcnt (* (count frames) (float frame-duration))}))

(defmethod schema/form :s/animation [_]
  [:map {:closed true}
   [:frames :some] ; FIXME actually images
   [:frame-duration pos?]
   [:looping? :boolean]])

(defmethod db/edn->value :s/animation [_ {:keys [frames frame-duration looping?]}]
  (create (map image/edn->image frames)
          :frame-duration frame-duration
          :looping? looping?))

(defmethod widget/create :s/animation [_ animation]
  (ui/table {:rows [(for [image (:frames animation)]
                      (ui/image-button (image/edn->image image)
                                       (fn on-clicked [])
                                       {:scale 2}))]
             :cell-defaults {:pad 1}}))

(defn- assoc-image-current-frame [entity animation]
  (assoc entity :entity/image (current-frame animation)))

(defmethod entity/create :entity/animation [[_ animation] eid]
  (swap! eid assoc-image-current-frame animation))

(defmethod entity/tick :entity/animation [[k animation] eid]
  (swap! eid #(-> %
                  (assoc-image-current-frame animation)
                  (assoc k (anim-tick animation world/delta)))))

(defmethod entity/create :entity/delete-after-animation-stopped [_ eid]
  (-> @eid :entity/animation :looping? not assert))

(defmethod entity/tick :entity/delete-after-animation-stopped [_ eid]
  (when (stopped? (:entity/animation @eid))
    (swap! eid assoc :entity/destroyed? true)))
