(ns cdq.tx.spawn-entity
  (:require [cdq.animation :as animation]
            [cdq.content-grid :as content-grid]
            [cdq.entity.state :as state]
            [cdq.inventory :as inventory]
            [cdq.grid :as grid]
            [cdq.malli :as m]
            [cdq.timer :as timer]
            [reduce-fsm :as fsm]
            [qrecord.core :as q]))

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(q/defrecord Body [body/position
                   body/width
                   body/height
                   body/collides?
                   body/z-order
                   body/rotation-angle])

(def entity-components
  {:entity/animation
   {:create   (fn [v _ctx]
                (animation/create v))}
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
   :entity/fsm                             {:create!  (fn [{:keys [fsm initial-state]} eid ctx]
                                                        ; fsm throws when initial-state is not part of states, so no need to assert initial-state
                                                        ; initial state is nil, so associng it. make bug report at reduce-fsm?
                                                        [[:tx/assoc eid :entity/fsm (assoc ((case fsm
                                                                                              :fsms/player player-fsm
                                                                                              :fsms/npc npc-fsm) initial-state nil) :state initial-state)]
                                                         [:tx/assoc eid initial-state (state/create ctx initial-state eid nil)]])}
   :entity/inventory                       {:create!  (fn [items eid _ctx]
                                                        (cons [:tx/assoc eid :entity/inventory inventory/empty-inventory]
                                                              (for [item items]
                                                                [:tx/pickup-item eid item])))}
   :entity/skills                          {:create!  (fn [skills eid _ctx]
                                                        (cons [:tx/assoc eid :entity/skills nil]
                                                              (for [skill skills]
                                                                [:tx/add-skill eid skill])))}})

(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/body :some]
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:creature/stats {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(q/defrecord Entity [entity/body])

(defn do!
  [[_ components]
   {:keys [ctx/id-counter
           ctx/entity-ids
           ctx/content-grid
           ctx/grid]
    :as ctx}]
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :entity/id))))
  (let [eid (atom (merge (map->Entity {})
                         (reduce (fn [m [k v]]
                                   (assoc m k (if-let [create (:create (k entity-components))]
                                                (create v ctx)
                                                v)))
                                 {}
                                 (assoc components :entity/id (swap! id-counter inc)))))]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid))
    (grid/add-entity! grid eid)
    (mapcat (fn [[k v]]
              (when-let [create! (:create! (k entity-components))]
                (create! v eid ctx)))
            @eid)))
