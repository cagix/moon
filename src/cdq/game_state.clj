(ns cdq.game-state
  (:require [cdq.animation :as animation]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.math :as math]
            [cdq.projectile :as projectile]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.timer :as timer]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.message]
            [cdq.vector2 :as v]
            [gdl.application]
            [gdl.c :as c]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :as utils]
            [reduce-fsm :as fsm]))

(defn- context-entity-moved! [{:keys [ctx/content-grid
                                      ctx/grid]}
                              eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))

(extend-type gdl.application.Context
  g/Context
  (context-entity-add! [{:keys [ctx/entity-ids
                                ctx/content-grid
                                ctx/grid]}
                        eid]
    (let [id (entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid/add-entity! grid eid))

  (context-entity-remove! [{:keys [ctx/entity-ids
                                   ctx/grid]}
                           eid]
    (let [id (entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid)))

(defn- move-entity! [ctx eid body direction rotate-in-movement-direction?]
  (context-entity-moved! ctx eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))

(defn- spawn-effect! [ctx position components]
  (g/spawn-entity! ctx
                   position
                   (g/config ctx :effect-body-props)
                   components))

(defn- player-entity-props [start-position {:keys [creature-id
                                                   free-skill-points
                                                   click-distance-tiles]}]
  {:position start-position
   :creature-id creature-id
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [ctx skill]
                                                 (action-bar/add-skill! (c/get-actor ctx :action-bar)
                                                                        skill))
                                 :skill-removed! (fn [ctx skill]
                                                   (action-bar/remove-skill! (c/get-actor ctx :action-bar)
                                                                             skill))
                                 :item-set! (fn [ctx inventory-cell item]
                                              (-> (c/get-actor ctx :windows)
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [ctx inventory-cell]
                                                  (-> (c/get-actor ctx :windows)
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- create-creature-body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn- spawn-creature! [ctx {:keys [position creature-id components]}]
  (let [props (g/build ctx creature-id)]
    (g/spawn-entity! ctx
                     position
                     (create-creature-body (:entity/body props))
                     (-> props
                         (dissoc :entity/body)
                         (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                         (utils/safe-merge components)))))

(defn- spawn-player-entity [ctx start-position]
  (spawn-creature! ctx
                   (player-entity-props (utils/tile->middle start-position)
                                        ctx/player-entity-config)))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(extend-type gdl.application.Context
  g/StageActors
  (open-error-window! [ctx throwable]
    (c/add-actor! ctx (error-window/create throwable)))

  (selected-skill [ctx]
    (action-bar/selected-skill (c/get-actor ctx :action-bar))))

(defmulti handle-tx! (fn [[k & _params] _ctx]
                       k))

(defmethod handle-tx! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value))

(defmethod handle-tx! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value))

(defmethod handle-tx! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k))

; => this means we dont want gdl context in our app open --- hide it again o.lo
(defmethod handle-tx! :tx/sound [[_ sound-name] ctx]
  (c/play-sound! ctx sound-name))

(defmethod handle-tx! :tx/set-cursor [[_ cursor] ctx]
  (c/set-cursor! ctx cursor))

(defmethod handle-tx! :tx/show-message [[_ message] ctx]
  (cdq.ui.message/show! (c/find-actor-by-name ctx "player-message")
                        message))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defmethod handle-tx! :tx/show-modal [[_ {:keys [title text button-text on-click]}] ctx]
  (assert (not (c/get-actor ctx ::modal)))
  (c/add-actor! ctx
                (ui/window {:title title
                            :rows [[(ui/label text)]
                                   [(ui/text-button button-text
                                                    (fn [_actor ctx]
                                                      (ui/remove! (c/get-actor ctx ::modal))
                                                      (on-click)))]]
                            :id ::modal
                            :modal? true
                            :center-position [(/ (c/ui-viewport-width ctx) 2)
                                              (* (c/ui-viewport-height ctx) (/ 3 4))]
                            :pack? true})))

(defmethod handle-tx! :tx/toggle-inventory-visible [_ ctx]
  (-> (c/get-actor ctx :windows)
      :inventory-window
      ui/toggle-visible!))

(defmethod handle-tx! :tx/audiovisual [[_ position {:keys [tx/sound entity/animation]}] ctx]
  (c/play-sound! ctx sound)
  (spawn-effect! ctx
                 position
                 {:entity/animation animation
                  :entity/delete-after-animation-stopped? true}))

(defmethod handle-tx! :tx/spawn-alert [[_ position faction duration] ctx]
  (spawn-effect! ctx
                 position
                 {:entity/alert-friendlies-after-duration
                  {:counter (g/create-timer ctx duration)
                   :faction faction}}))

(defmethod handle-tx! :tx/spawn-creature [[_ opts] ctx]
  (spawn-creature! ctx opts))

(defmethod handle-tx! :tx/spawn-item [[_ position item] ctx]
  (g/spawn-entity! ctx
                   position
                   {:width 0.75
                    :height 0.75
                    :z-order :z-order/on-ground}
                   {:entity/image (:entity/image item)
                    :entity/item item
                    :entity/clickable {:type :clickable/item
                                       :text (:property/pretty-name item)}}))

(defmethod handle-tx! :tx/spawn-line [[_ {:keys [start end duration color thick?]}] ctx]
  (spawn-effect! ctx
                 start
                 #:entity {:line-render {:thick? thick? :end end :color color}
                           :delete-after-duration duration}))

(defmethod handle-tx! :tx/spawn-projectile [[_
                                             {:keys [position direction faction]}
                                             {:keys [entity/image
                                                     projectile/max-range
                                                     projectile/speed
                                                     entity-effects
                                                     projectile/piercing?] :as projectile}]
                                            ctx]
  (let [size (projectile/size projectile)]
    (g/spawn-entity! ctx
                     position
                     {:width size
                      :height size
                      :z-order :z-order/flying
                      :rotation-angle (v/angle-from-vector direction)}
                     {:entity/movement {:direction direction
                                        :speed speed}
                      :entity/image image
                      :entity/faction faction
                      :entity/delete-after-duration (/ max-range speed)
                      :entity/destroy-audiovisual :audiovisuals/hit-wall
                      :entity/projectile-collision {:entity-effects entity-effects
                                                    :piercing? piercing?}})))

(defmethod handle-tx! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(g/handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))

(defmethod handle-tx! :tx/event [[_ eid event params] ctx]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (entity/state-obj @eid)
              new-state-obj [new-state-k (entity/create (if params
                                                          [new-state-k eid params]
                                                          [new-state-k eid])
                                                        ctx)]]
          (when (:entity/player? @eid)
            (g/handle-txs! ctx ((:state-changed! (:entity/player? @eid)) new-state-obj)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (g/handle-txs! ctx (state/exit!  old-state-obj eid ctx))
          (g/handle-txs! ctx (state/enter! new-state-obj eid)))))))

(defmethod handle-tx! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true))

(defmethod handle-tx! :tx/update-animation [[_ eid animation] {:keys [ctx/delta-time]}]
  (swap! eid #(-> %
                  (assoc :entity/image (animation/current-frame animation))
                  (assoc :entity/animation (animation/tick animation delta-time)))))

(defmethod handle-tx! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers))

(defmethod handle-tx! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers))

(defmethod handle-tx! :tx/move-entity [[_ & params] ctx]
  (apply move-entity! ctx params))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npc/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npc/player movement 'state' and no movement 'component' necessary !
; for projectiles inside projectile update !?
(defn- set-movement* [entity movement-vector]
  (assoc entity :entity/movement {:direction movement-vector
                                  :speed (or (entity/stat entity :entity/movement-speed) 0)}))

(defmethod handle-tx! :tx/set-movement [[_ eid movement-vector] _ctx]
  (swap! eid set-movement* movement-vector))

(defmethod handle-tx! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost))

(defmethod handle-tx! :tx/set-item [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      ((:item-set! (:entity/player? entity)) ctx cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defmethod handle-tx! :tx/remove-item [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      ((:item-removed! (:entity/player? entity)) ctx cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (cdq.tx.remove-item/do! eid cell)
       (cdq.tx.set-item/do! eid cell (update item :count dec)))
      (cdq.tx.remove-item/do! eid cell))))

(defmethod handle-tx! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (handle-tx! [:tx/set-item eid cell item] ctx))))

; TODO no items which stack are available
#_(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (cdq.tx.remove-item/do! eid cell)
            (cdq.tx.set-item/do! eid cell (update cell-item :count + (:count item))))))

(defn do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))

(defmethod handle-tx! :tx/set-cooldown [[_ eid skill] ctx]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (g/create-timer ctx (:skill/cooldown skill))))

(defmethod handle-tx! :tx/add-skill [[_ eid {:keys [property/id] :as skill}] ctx]
  {:pre [(not (contains? (:entity/skills @eid) id))]}
  (when (:entity/player? @eid)
    ((:skill-added! (:entity/player? @eid)) ctx skill))
  (swap! eid assoc-in [:entity/skills id] skill))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (when (:entity/player? @eid)
      ((:skill-removed! (:entity/player? @eid)) ctx skill))
    (swap! eid update :entity/skills dissoc id))

(defn- add-text-effect* [entity text ctx]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(g/reset-timer ctx %)))
           {:text text
            :counter (g/create-timer ctx 0.4)})))

(defmethod handle-tx! :tx/add-text-effect [[_ eid text] ctx]
  (swap! eid add-text-effect* text ctx))

(extend-type gdl.application.Context
  g/EffectHandler
  (handle-txs! [ctx transactions]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  ]]
      (try (handle-tx! transaction ctx)
           (catch Throwable t
             (throw (ex-info "" {:transaction transaction} t)))))))

(extend-type gdl.application.Context
  g/Raycaster
  (ray-blocked? [{:keys [ctx/raycaster]} start end]
    (raycaster/blocked? raycaster
                        start
                        end))

  (path-blocked? [{:keys [ctx/raycaster]} start end width]
    (raycaster/path-blocked? raycaster
                             start
                             end
                             width)))

(extend-type gdl.application.Context
  g/Time
  (elapsed-time [{:keys [ctx/elapsed-time]}]
    elapsed-time)

  (create-timer [{:keys [ctx/elapsed-time]} duration]
    (timer/create elapsed-time duration))

  (timer-stopped? [{:keys [ctx/elapsed-time]} timer]
    (timer/stopped? elapsed-time timer))

  (reset-timer [{:keys [ctx/elapsed-time]} timer]
    (timer/reset elapsed-time timer))

  (timer-ratio [{:keys [ctx/elapsed-time]} timer]
    (timer/ratio elapsed-time timer)))

(extend-type gdl.application.Context
  cdq.g/Grid
  (grid-cell [{:keys [ctx/grid]} position]
    (grid/cell grid position))

  (point->entities [{:keys [ctx/grid]} position]
    (grid/point->entities grid position))

  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->cells [{:keys [ctx/grid]} circle]
    (grid/circle->cells grid circle))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(defprotocol StageActors
  (create-actors [_]))

(defn create! [ctx]
  (c/reset-actors! ctx (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((g/config ctx :world-fn) ctx)
        grid (grid-impl/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create tiled-map (g/config ctx :content-grid-cell-size))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))
