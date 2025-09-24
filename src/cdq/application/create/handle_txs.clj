(ns cdq.application.create.handle-txs
  (:require [cdq.audio :as audio]
            [cdq.creature :as creature]
            [cdq.ctx]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.animation :as animation]
            [cdq.entity.body :as body]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.entity.stats]
            [cdq.graphics :as graphics]
            [cdq.malli :as m]
            [cdq.stage]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clj-commons.pretty.repl :as pretty-repl]
            [clojure.math.vector2 :as v]
            [clojure.repl]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.stage :as stage]
            [clojure.tx-handler :as tx-handler]
            [clojure.utils :as utils]
            [qrecord.core :as q]))

(def ^:private create-fns
  {:entity/animation animation/create
   :entity/body      body/create
   :entity/delete-after-duration (fn [duration {:keys [ctx/world]}]
                                   (timer/create (:world/elapsed-time world) duration))
   :entity/projectile-collision (fn [v _ctx]
                                  (assoc v :already-hit-bodies #{}))
   :creature/stats cdq.entity.stats/create})

(defn- create-component [[k v] ctx]
  (if-let [f (create-fns k)]
    (f v ctx)
    v))

(defn- create-fsm
  [{:keys [fsm initial-state]}
   eid
   {:keys [ctx/world]}]
  ; fsm throws when initial-state is not part of states, so no need to assert initial-state
  ; initial state is nil, so associng it. make bug report at reduce-fsm?
  [[:tx/assoc eid :entity/fsm (assoc ((get (:world/fsms world) fsm) initial-state nil) :state initial-state)]
   [:tx/assoc eid initial-state (state/create [initial-state nil] eid world)]])

(defn- create!-inventory [items eid _ctx]
  (cons [:tx/assoc eid :entity/inventory (inventory/create)]
        (for [item items]
          [:tx/pickup-item eid item])))

(def ^:private create!-fns
  {:entity/fsm                             create-fsm
   :entity/inventory                       create!-inventory
   :entity/delete-after-animation-stopped? (fn [_ eid _ctx]
                                             (-> @eid :entity/animation :looping? not assert)
                                             nil)
   :entity/skills                          (fn [skills eid _ctx]
                                             (cons [:tx/assoc eid :entity/skills nil]
                                                   (for [skill skills]
                                                     [:tx/add-skill eid skill])))})

(defn- after-create-component [[k v] eid ctx]
  (when-let [f (create!-fns k)]
    (f v eid ctx)))

(q/defrecord Entity [entity/body]
  entity/Entity
  (position [_]
    (:body/position body))

  (distance [_ other-entity]
    (body/distance body
                   (:entity/body other-entity))))

(extend-type Entity
  creature/Skills
  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (stats/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (seq (filter #(effect/applicable? % effect-ctx) effects)))
     :invalid-params

     :else
     :usable)))

(def ^:private txs-fn-map
  {
   :tx/sound (fn [{:keys [ctx/audio]} sound-name]
               (audio/play-sound! audio sound-name)
               nil)

   :tx/assoc (fn [_ctx eid k value]
               (swap! eid assoc k value)
               nil)

   :tx/assoc-in (fn [_ctx eid ks value]
                  (swap! eid assoc-in ks value)
                  nil)

   :tx/dissoc (fn [_ctx eid k]
                (swap! eid dissoc k)
                nil)

   :tx/mark-destroyed (fn [_ctx eid]
                        (swap! eid assoc :entity/destroyed? true)
                        nil)

   :tx/mod-add (fn [_ctx eid modifiers]
                 (swap! eid update :creature/stats stats/add modifiers)
                 nil)

   :tx/mod-remove (fn [_ctx eid modifiers]
                    (swap! eid update :creature/stats stats/remove-mods modifiers)
                    nil)

   :tx/pay-mana-cost (fn [_ctx eid cost]
                       (swap! eid update :creature/stats stats/pay-mana-cost cost)
                       nil)

   :tx/set-cooldown (fn [{:keys [ctx/world]} eid skill]
                      (swap! eid assoc-in
                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                             (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
                      nil)

   :tx/add-text-effect (fn [{:keys [ctx/world]} eid text duration]
                         [[:tx/assoc
                           eid
                           :entity/string-effect
                           (if-let [existing (:entity/string-effect @eid)]
                             (-> existing
                                 (update :text str "\n" text)
                                 (update :counter timer/increment duration))
                             {:text text
                              :counter (timer/create (:world/elapsed-time world) duration)})]])

   :tx/add-skill (fn [_ctx eid {:keys [property/id] :as skill}]
                   {:pre [(not (contains? (:entity/skills @eid) id))]}
                   (swap! eid update :entity/skills assoc id skill)
                   (if (:entity/player? @eid)
                     [[:tx/player-add-skill skill]]
                     nil))

   #_(defn remove-skill [_ctx eid {:keys [property/id] :as skill}]
       {:pre [(contains? (:entity/skills @eid) id)]}
       (swap! eid update :entity/skills dissoc id)
       (when (:entity/player? @eid)
         (remove-skill! ctx skill)))

   :tx/set-item (fn [_ctx eid cell item]
                  (let [entity @eid
                        inventory (:entity/inventory entity)]
                    (assert (and (nil? (get-in inventory cell))
                                 (inventory/valid-slot? cell item)))
                    (swap! eid assoc-in (cons :entity/inventory cell) item)
                    (when (inventory/applies-modifiers? cell)
                      (swap! eid update :creature/stats stats/add (:entity/modifiers item)))
                    (if (:entity/player? entity)
                      [[:tx/player-set-item cell item]]
                      nil)))

   :tx/remove-item (fn [_ctx eid cell]
                     (let [entity @eid
                           item (get-in (:entity/inventory entity) cell)]
                       (assert item)
                       (swap! eid assoc-in (cons :entity/inventory cell) nil)
                       (when (inventory/applies-modifiers? cell)
                         (swap! eid update :creature/stats stats/remove-mods (:entity/modifiers item)))
                       (if (:entity/player? entity)
                         [[:tx/player-remove-item cell]]
                         nil)))

   :tx/pickup-item (fn [_ctx eid item]
                     (inventory/assert-valid-item? item)
                     (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
                       (assert cell)
                       (assert (or (inventory/stackable? item cell-item)
                                   (nil? cell-item)))
                       (if (inventory/stackable? item cell-item)
                         (do
                          #_(tx/stack-item ctx eid cell item))
                         [[:tx/set-item eid cell item]])))

   :tx/event (fn [{:keys [ctx/world]} & params]
               (apply world/handle-event world params))

   :tx/state-exit (fn [ctx eid [state-k state-v]]
                    (state/exit [state-k state-v] eid ctx))

   :tx/state-enter (fn [_ctx eid [state-k state-v]]
                     (state/enter [state-k state-v] eid))

   :tx/effect (fn [{:keys [ctx/world]} effect-ctx effects]
                (mapcat #(effect/handle % effect-ctx world)
                        (filter #(effect/applicable? % effect-ctx) effects)))

   :tx/print-stacktrace (let[print-level 3
                             print-depth 24]
                          (fn [_ctx throwable]
                            (binding [*print-level* print-level]
                              (pretty-repl/pretty-pst throwable print-depth))
                            nil))

   :tx/show-error-window (fn [{:keys [ctx/stage]} throwable]
                           (stage/add! stage (scene2d/build
                                              {:actor/type :actor.type/window
                                               :title "Error"
                                               :rows [[{:actor {:actor/type :actor.type/label
                                                                :label/text (binding [*print-level* 3]
                                                                              (utils/with-err-str
                                                                                (clojure.repl/pst throwable)))}}]]
                                               :modal? true
                                               :close-button? true
                                               :close-on-escape? true
                                               :center? true
                                               :pack? true})))

   :tx/player-add-skill (fn [{:keys [ctx/graphics
                                     ctx/stage]}
                             skill]
                          (cdq.stage/add-skill! stage
                                            {:skill-id (:property/id skill)
                                             :texture-region (graphics/texture-region graphics (:entity/image skill))
                                             :tooltip-text (fn [{:keys [ctx/world]}]
                                                             (world/info-text world skill))})
                          nil)

   :tx/player-set-item (fn [{:keys [ctx/graphics
                                    ctx/stage]}
                            cell item]
                         (cdq.stage/set-item! stage cell
                                          {:texture-region (graphics/texture-region graphics (:entity/image item))
                                           :tooltip-text (fn [{:keys [ctx/world]}]
                                                           (world/info-text world item))})
                         nil)

   :tx/player-remove-item (fn [{:keys [ctx/stage]}
                               cell]
                            (cdq.stage/remove-item! stage cell)
                            nil)

   :tx/toggle-inventory-visible (fn [{:keys [ctx/stage]}]
                                  (cdq.stage/toggle-inventory-visible! stage)
                                  nil)

   :tx/show-message (fn [{:keys [ctx/stage]} message]
                      (cdq.stage/show-text-message! stage message)
                      nil)

   :tx/show-modal (fn [{:keys [ctx/stage]} opts]
                    (cdq.stage/show-modal-window! stage (clojure.scene2d.stage/viewport stage) opts)
                    nil)

   :tx/audiovisual (fn [{:keys [ctx/db]} position audiovisual]
                     (let [{:keys [tx/sound
                                   entity/animation]} (if (keyword? audiovisual)
                                                        (db/build db audiovisual)
                                                        audiovisual)]
                       [[:tx/sound sound]
                        [:tx/spawn-effect
                         position
                         {:entity/animation animation
                          :entity/delete-after-animation-stopped? true}]]))

   :tx/spawn-alert (fn [{:keys [ctx/world]} position faction duration]
                     [[:tx/spawn-effect
                       position
                       {:entity/alert-friendlies-after-duration
                        {:counter (timer/create (:world/elapsed-time world) duration)
                         :faction faction}}]])

   :tx/spawn-line (fn [_ctx {:keys [start end duration color thick?]}]
                    [[:tx/spawn-effect
                      start
                      {:entity/line-render {:thick? thick? :end end :color color}
                       :entity/delete-after-duration duration}]])

   :tx/move-entity (fn [{:keys [ctx/world]} eid body direction rotate-in-movement-direction?]
                     (let [{:keys [world/content-grid
                                   world/grid]} world]
                       (content-grid/position-changed! content-grid eid)
                       (grid/remove-from-touched-cells! grid eid)
                       (grid/set-touched-cells! grid eid)
                       (when (:body/collides? (:entity/body @eid))
                         (grid/remove-from-occupied-cells! grid eid)
                         (grid/set-occupied-cells! grid eid)))
                     (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
                     (when rotate-in-movement-direction?
                       (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
                     nil)

   :tx/spawn-projectile (fn [_ctx
                             {:keys [position direction faction]}
                             {:keys [entity/image
                                     projectile/max-range
                                     projectile/speed
                                     entity-effects
                                     projectile/size
                                     projectile/piercing?] :as projectile}]
                          [[:tx/spawn-entity
                            {:entity/body {:position position
                                           :width size
                                           :height size
                                           :z-order :z-order/flying
                                           :rotation-angle (v/angle-from-vector direction)}
                             :entity/movement {:direction direction
                                               :speed speed}
                             :entity/image image
                             :entity/faction faction
                             :entity/delete-after-duration (/ max-range speed)
                             :entity/destroy-audiovisual :audiovisuals/hit-wall
                             :entity/projectile-collision {:entity-effects entity-effects
                                                           :piercing? piercing?}}]])

   :tx/spawn-effect (fn [{:keys [ctx/world]}
                         position
                         components]
                      [[:tx/spawn-entity
                        (assoc components
                               :entity/body (assoc (:world/effect-body-props world) :position position))]])

   :tx/spawn-item (fn [_ctx position item]
                    [[:tx/spawn-entity
                      {:entity/body {:position position
                                     :width 0.75
                                     :height 0.75
                                     :z-order :z-order/on-ground}
                       :entity/image (:entity/image item)
                       :entity/item item
                       :entity/clickable {:type :clickable/item
                                          :text (:property/pretty-name item)}}]])

   ; # :z-order/flying has no effect for now
   ; * entities with :z-order/flying are not flying over water,etc. (movement/air)
   ; because using potential-field for z-order/ground
   ; -> would have to add one more potential-field for each faction for z-order/flying
   ; * they would also (maybe) need a separate occupied-cells if they don't collide with other
   ; * they could also go over ground units and not collide with them
   ; ( a test showed then flying OVER player entity )
   ; -> so no flying units for now
   :tx/spawn-creature (fn [_ctx
                           {:keys [position
                                   creature-property
                                   components]}]
                        (assert creature-property)
                        [[:tx/spawn-entity
                          (-> creature-property
                              (assoc :entity/body (let [{:keys [body/width body/height #_body/flying?]} (:entity/body creature-property)]
                                                    {:position position
                                                     :width  width
                                                     :height height
                                                     :collides? true
                                                     :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)}))
                              (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                              (utils/safe-merge components))]])

   :tx/spawn-entity (fn [{:keys [ctx/world]
                          :as ctx}
                         entity]
                      (let [{:keys [world/content-grid
                                    world/entity-ids
                                    world/grid
                                    world/id-counter
                                    world/spawn-entity-schema]} world
                            _ (m/validate-humanize spawn-entity-schema entity)
                            entity (reduce (fn [m [k v]]
                                             (assoc m k (create-component [k v] ctx)))
                                           {}
                                           entity)
                            _ (assert (and (not (contains? entity :entity/id))))
                            entity (assoc entity :entity/id (swap! id-counter inc))
                            entity (merge (map->Entity {}) entity)
                            eid (atom entity)]
                        (let [id (:entity/id @eid)]
                          (assert (number? id))
                          (swap! entity-ids assoc id eid))
                        (content-grid/add-entity! content-grid eid)
                        ; https://github.com/damn/core/issues/58
                        ;(assert (valid-position? grid @eid))
                        (grid/set-touched-cells! grid eid)
                        (when (:body/collides? (:entity/body @eid))
                          (grid/set-occupied-cells! grid eid))
                        (mapcat #(after-create-component % eid ctx) @eid)))
   }
  )

(defn do! [ctx]
  (extend-type (class ctx)
    cdq.ctx/TransactionHandler
    (handle-txs! [ctx transactions]
      (tx-handler/actions! txs-fn-map ctx transactions)))
  ctx)
