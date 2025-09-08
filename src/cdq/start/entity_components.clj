(ns cdq.start.entity-components
  (:require [cdq.animation :as animation]
            [cdq.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.timer :as timer]
            [qrecord.core :as q]))

(defrecord Animation [frames frame-duration looping? cnt maxcnt]
  cdq.animation/Animation
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
  (map->Animation
   {:frames (vec frames)
    :frame-duration frame-duration
    :looping? looping?
    :cnt 0
    :maxcnt (* (count frames) (float frame-duration))}))

(q/defrecord Body [body/position
                   body/width
                   body/height
                   body/collides?
                   body/z-order
                   body/rotation-angle])

(def method-mappings
  {:entity/animation
   {:create   (fn [v _ctx]
                (create-animation v))}
   :entity/body                            {:create   (fn [{[x y] :position
                                                            :keys [position
                                                                   width
                                                                   height
                                                                   collides?
                                                                   z-order
                                                                   rotation-angle]}
                                                           {:keys [ctx/minimum-size
                                                                   ctx/z-orders]}]
                                                        (assert position)
                                                        (assert width)
                                                        (assert height)
                                                        (assert (>= width  (if collides? minimum-size 0)))
                                                        (assert (>= height (if collides? minimum-size 0)))
                                                        (assert (or (boolean? collides?) (nil? collides?)))
                                                        (assert ((set z-orders) z-order))
                                                        (assert (or (nil? rotation-angle)
                                                                    (<= 0 rotation-angle 360)))
                                                        (map->Body
                                                         {:position (mapv float position)
                                                          :width  (float width)
                                                          :height (float height)
                                                          :collides? collides?
                                                          :z-order z-order
                                                          :rotation-angle (or rotation-angle 0)}))}
   :entity/delete-after-animation-stopped? {:create!  (fn [_ eid _ctx]
                                                        (-> @eid :entity/animation :looping? not assert)
                                                        nil)}
   :entity/delete-after-duration           {:create   (fn [duration {:keys [ctx/elapsed-time]}]
                                                        (timer/create elapsed-time duration))}
   :entity/projectile-collision            {:create   (fn create [v _ctx]
                                                        (assoc v :already-hit-bodies #{}))}
   :creature/stats                         {:create   (fn [stats _ctx]
                                                        (-> (if (:entity/mana stats)
                                                              (update stats :entity/mana (fn [v] [v v]))
                                                              stats)
                                                            (update :entity/hp   (fn [v] [v v])))
                                                        #_(-> stats
                                                              (update :entity/mana (fn [v] [v v])) ; TODO is OPTIONAL ! then making [nil nil]
                                                              (update :entity/hp   (fn [v] [v v]))))}
   :entity/fsm                             {:create!  (fn [{:keys [fsm initial-state]} eid {:keys [ctx/fsms]
                                                                                            :as ctx}]
                                                        ; fsm throws when initial-state is not part of states, so no need to assert initial-state
                                                        ; initial state is nil, so associng it. make bug report at reduce-fsm?
                                                        [[:tx/assoc eid :entity/fsm (assoc ((get fsms fsm) initial-state nil) :state initial-state)]
                                                         [:tx/assoc eid initial-state (state/create ctx initial-state eid nil)]])}
   :entity/inventory                       {:create!  (fn [items eid _ctx]
                                                        (cons [:tx/assoc eid :entity/inventory inventory/empty-inventory]
                                                              (for [item items]
                                                                [:tx/pickup-item eid item])))}
   :entity/skills                          {:create!  (fn [skills eid _ctx]
                                                        (cons [:tx/assoc eid :entity/skills nil]
                                                              (for [skill skills]
                                                                [:tx/add-skill eid skill])))}})

(defn do! [ctx]
  (assoc ctx :ctx/entity-components method-mappings))
