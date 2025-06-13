(ns cdq.world
  (:require [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.inventory :as inventory]
            [cdq.malli :as m]
            [cdq.math.geom :as geom]
            [cdq.modifiers :as modifiers]
            [cdq.rand :refer [rand-int-between]]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [gdl.math.vector2 :as v]
            [qrecord.core :as q]
            [reduce-fsm :as fsm]))

(defn- add-skill [skills {:keys [property/id] :as skill}]
  {:pre [(not (contains? skills id))]}
  (assoc skills id skill))

(defn- add-text-effect* [entity text {:keys [ctx/elapsed-time]}]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter #(timer/reset elapsed-time %)))
           {:text text
            :counter (timer/create elapsed-time 0.4)})))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defmethod do! :tx/toggle-inventory-visible [_ _ctx]
  [:world.event/toggle-inventory-visible])

(defmethod do! :tx/show-message [[_ message] _ctx]
  [:world.event/show-player-message message])

(defmethod do! :tx/show-modal [[_ opts] _ctx]
  [:world.event/show-modal-window opts])

(defmethod do! :tx/sound [[_ sound-name] _ctx]
  [:world.event/sound sound-name])

(defn- valid-tx? [transaction]
  (vector? transaction))

(defn- handle-world-event!
  [{:keys [ctx/world-event-handlers]
    :as ctx}
   [world-event-k params]]
  ((utils/safe-get world-event-handlers world-event-k) ctx params))

(comment

 ; # handle-txs! takes what ? what are 'txs' ?
 ; => either nil or something which `do!` understands - no default behaviour.

 ; # do! should return what?
 ; * document
 ; * validate
 ; * test ( e.g. tx/effect or tx/event - e.g. pass abstract fsm)

 ; # Research
 ; 1. RETURN `nil` (does something)

 ; 2. Returns one world event `[:world.event/set-cursor cursor-key]`

 ; Exceptions:
 ; * :tx/effect ( make testable ! )

 ; -> handle-txs! -> returns new transactions to be handled


 ; * :tx/event ( make testable ! )

 ; does something (swap!)
 ; handles txs of state/exit! ( which could also contain world events ...)
 ; handles txs of state/enter!
 ; returns world event

 ; * :tx/audiovisual

 ; => returns two other txs

 ; * :tx/spawn-alert

 ; => return other txs

 ; * :tx/spawn-line
 ; * :tx/deal-damage
 ; * :tx/spawn-projectile
 ; * :tx/spawn-effect
 ; * :tx/spawn-item
 ; * :tx/spawn-creature


 ; ## :tx/spawn-creature
 ; -> spawn-creature!
 ; -> spawn-entity!

 ; ## spawn-entity!
 ; -> does something _AND_ returns transactions _AND_ those transactions might trigger world events !
 ; 1. increments id-counter
 ; 2. context-entity-add!
 ; 3. handle-txs!
 ; 4. returns eid

 )

; TODO :tx/spawn-projectile calls spawn-entity
; which returns a list of world-events
; so `do!` can return more than one result ....
; => fix it & assert it what `do!` can return ...
(defn handle-txs!
  "Handles transactions (side-effects) on the world and returns a list of world-events."
  [ctx
   transactions]
  ;(println "\n~handle-txs! " (map first (remove nil? transactions)))
  ;(println "~\n")
  (let [world-events (remove nil?
                             (for [transaction transactions
                                   :when transaction]
                               (do
                                (assert (valid-tx? transaction) (pr-str transaction))
                                (try (do! transaction ctx)
                                     (catch Throwable t
                                       (throw (ex-info "" {:transaction transaction} t)))))))]
    (binding [*print-level* 2]
      ;(println "\n~world-events: " world-events)
      ;(println "~\n")
      )
    (run! (partial handle-world-event! ctx) world-events)))

(defmethod do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)

(defmethod do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)

(defmethod do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)

(defmethod do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)

(defmethod do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers)
  nil)

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers)
  nil)

; HOW CAN WE TEST THIS ?
; HAVE TO DEFINE MULTIMETHODS ...
(defmethod do! :tx/effect [[_ effect-ctx effects] ctx]
  (run! #(handle-txs! ctx (effect/handle % effect-ctx ctx))
        (effect/filter-applicable? effect-ctx effects)))

(defmethod do! :tx/event [[_ eid event params] ctx]
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
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (handle-txs! ctx (state/exit!  old-state-obj eid ctx))
          (handle-txs! ctx (state/enter! new-state-obj eid))
          nil)))))

(defmethod do! :tx/add-skill [[_ eid skill] _ctx]
  (swap! eid update :entity/skills add-skill skill)
  (when (:entity/player? @eid)
    [:world.event/player-skill-added skill]))

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      [:world.event/player-skill-removed skill]))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/elapsed-time]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create elapsed-time (:skill/cooldown skill)))
  nil)

(defmethod do! :tx/add-text-effect [[_ eid text] ctx]
  (swap! eid add-text-effect* text ctx)
  nil)

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)

(defmethod do! :tx/set-item [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))
    (when (:entity/player? entity)
      [:world.event/player-item-set [cell item]])))

(defmethod do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (do! [:tx/set-item eid cell item] ctx))))

(defmethod do! :tx/remove-item [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))
    (when (:entity/player? entity)
      [:world.event/player-item-removed cell])))

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

; TODO no items which stack are available
#_(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (cdq.tx.remove-item/do! eid cell)
            (cdq.tx.set-item/do! eid cell (update cell-item :count + (:count item))))))

#_(defn do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))

(defmethod do! :tx/audiovisual [[_ position audiovisual]
                                {:keys [ctx/db]
                                 :as ctx}]
  (let [{:keys [tx/sound
                entity/animation]} (if (keyword? audiovisual)
                                     (db/build db audiovisual)
                                     audiovisual)]
    (do! [:tx/sound sound]
         ctx)
    (do! [:tx/spawn-effect
          position
          {:entity/animation animation
           :entity/delete-after-animation-stopped? true}]
         ctx)
    nil))

(defmethod do! :tx/spawn-alert [[_ position faction duration]
                                {:keys [ctx/elapsed-time] :as ctx}]
  (do! [:tx/spawn-effect
        position
        {:entity/alert-friendlies-after-duration
         {:counter (timer/create elapsed-time duration)
          :faction faction}}]
       ctx)
  nil)

(defmethod do! :tx/spawn-line [[_ {:keys [start end duration color thick?]}] ctx]
  (do! [:tx/spawn-effect
        start
        {:entity/line-render {:thick? thick? :end end :color color}
         :entity/delete-after-duration duration}]
       ctx)
  nil)

(defmethod do! :tx/deal-damage [[_ source target damage] ctx]
  (let [source* @source
        target* @target
        hp (entity/hitpoints target*)]
    (handle-txs! ctx
                 (cond
                  (zero? (hp 0))
                  nil

                  (< (rand) (modifiers/effective-armor-save (:creature/stats source*)
                                                            (:creature/stats target*)))
                  [[:tx/add-text-effect target "[WHITE]ARMOR"]]

                  :else
                  (let [min-max (:damage/min-max (modifiers/damage (:creature/stats source*)
                                                                   (:creature/stats target*)
                                                                   damage))
                        dmg-amount (rand-int-between min-max)
                        new-hp-val (max (- (hp 0) dmg-amount)
                                        0)]
                    [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
                     [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
                     [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
                     [:tx/add-text-effect target (str "[RED]" dmg-amount "[]")]])))))

(defn- context-entity-add! [{:keys [ctx/entity-ids
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

(defn context-entity-remove! [{:keys [ctx/entity-ids
                                      ctx/grid]}
                              eid]
  (let [id (entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! grid eid))

(defn- context-entity-moved! [{:keys [ctx/content-grid
                                     ctx/grid]}
                             eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))

(defmethod do! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (context-entity-moved! ctx eid)
  (swap! eid assoc
         :entity/position (:entity/position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))
  nil)

(defmethod do! :tx/set-movement [[_ eid movement-vector] _ctx]
  (swap! eid entity/set-movement movement-vector)
  nil)

; TODO what about components which get added later/??
; => validate?
; => :entity/id ... body
(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/image {:optional true} :some] ; what do we really want to do - draw a creature or projectile as of 'type' ??? game world logic should not contain graphical representation ...
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
             [:creature/stats {:optional true} :some] ; only for creatures thats why optional -> create separate schema ther
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))


; :body/foo ?


; 1. make namespaced keys
; start with position !!!

; also cell ...  !! impossible to find ...
; what other 'records' /key words do I have ?

; math.geom rectangle->tiles uses actually entity / body !
; with 'left-bottom' ...

(q/defrecord Body [entity/position
                   left-bottom ; dry

                   width
                   height
                   half-width ; dry
                   half-height ; dry
                   radius ; ??

                   collides?
                   z-order
                   rotation-angle]
  entity/Entity
  (position [_]
    position)

  (rectangle [_]
    (let [[x y] left-bottom]
      (geom/rectangle x y width height)))

  (overlaps? [this other-entity]
    (geom/overlaps? (entity/rectangle this)
                    (entity/rectangle other-entity)))

  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (entity/position entity)
                             (entity/position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange)))

  (id [{:keys [entity/id]}]
    id)

  (faction [{:keys [entity/faction]}]
    faction)

  (enemy [this]
    (case (entity/faction this)
      :evil :good
      :good :evil))

  (state-k [{:keys [entity/fsm]}]
    (:state fsm))

  (state-obj [this]
    (let [k (entity/state-k this)]
      [k (k this)]))

  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (modifiers/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable))

  (mod-add    [entity mods] (update entity :creature/stats modifiers/add    mods))
  (mod-remove [entity mods] (update entity :creature/stats modifiers/remove mods))

  (stat [this k]
    (modifiers/get-stat-value (:creature/stats this) k))

  (mana [entity]
    (modifiers/get-mana (:creature/stats entity)))

  (mana-val [entity]
    (modifiers/mana-val (:creature/stats entity)))

  (hitpoints [entity]
    (modifiers/get-hitpoints (:creature/stats entity)))

  (pay-mana-cost [entity cost]
    (update entity :creature/stats modifiers/pay-mana-cost cost)))

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}
                    minimum-size
                    z-orders]
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
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components ctx]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] ctx)))
          {}
          components))

(defn- spawn-entity!
  [{:keys [ctx/id-counter
           ctx/z-orders
           ctx/minimum-size]
    :as ctx}
   position
   body
   components]
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      (create-body minimum-size z-orders)
                      (utils/safe-merge (-> components
                                            (assoc :entity/id (swap! id-counter inc))
                                            (create-vs ctx)))))]
    (context-entity-add! ctx eid)
    (->> @eid
         (mapcat #(entity/create! % eid ctx))
         (handle-txs! ctx))
    eid))

(defmethod do! :tx/spawn-projectile
  [[_
    {:keys [position direction faction]}
    {:keys [entity/image
            projectile/max-range
            projectile/speed
            entity-effects
            projectile/size
            projectile/piercing?] :as projectile}]
   ctx]
  (spawn-entity! ctx
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
                                                :piercing? piercing?}})
  nil)

(defmethod do! :tx/spawn-effect
  [[_ position components]
   {:keys [ctx/config]
    :as ctx}]
  (spawn-entity! ctx
                 position
                 (:effect-body-props config)
                 components)
  nil)

(defmethod do! :tx/spawn-item [[_ position item] ctx]
  (spawn-entity! ctx
                 position
                 {:width 0.75
                  :height 0.75
                  :z-order :z-order/on-ground}
                 {:entity/image (:entity/image item)
                  :entity/item item
                  :entity/clickable {:type :clickable/item
                                     :text (:property/pretty-name item)}})
  nil)


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

(defn spawn-creature! [ctx
                       {:keys [position
                               creature-property
                               components]}]
  (assert creature-property)
  (let [props creature-property]
    (spawn-entity! ctx
                   position
                   (create-creature-body (:entity/body props))
                   (-> props
                       (dissoc :entity/body)
                       (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                       (utils/safe-merge components)))))

(defmethod do! :tx/spawn-creature [[_ opts] ctx]
  (spawn-creature! ctx opts)
  nil)

(defn create [ctx config {:keys [tiled-map
                                 start-position
                                 creatures
                                 player-entity]}]
  (let [grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map ; only @ cdq.render.draw-world-map -> pass graphics??
                    :ctx/elapsed-time 0 ; -> everywhere
                    :ctx/grid grid ; -> everywhere -> abstract ?
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                           (:tiled-map/height tiled-map)
                                                           (:content-grid-cell-size config))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                                                                      (:tiled-map/height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)
                    :ctx/factions-iterations (:potential-field-factions-iterations config)
                    :ctx/z-orders z-orders
                    :ctx/render-z-order (utils/define-order z-orders)
                    :ctx/minimum-size minimum-size
                    :ctx/max-delta max-delta
                    :ctx/max-speed max-speed})
        ctx (assoc ctx :ctx/player-eid (spawn-creature! ctx player-entity))]
    (run! (partial spawn-creature! ctx) creatures)
    ctx))

(defn calculate-active-entities [{:keys [ctx/content-grid
                                         ctx/player-eid]}
                                 _params]
  (content-grid/active-entities content-grid @player-eid))
