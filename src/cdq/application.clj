(ns cdq.application
  (:require [cdq.animation :as animation]
            [cdq.application.config :as config]
            [cdq.application.db :as db]
            [cdq.application.ctx-schema :as ctx-schema]
            [cdq.application.potential-fields.update :as potential-fields.update]
            [cdq.application.potential-fields.movement :as potential-fields.movement]
            [cdq.application.raycaster :as raycaster]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.malli :as m]
            [cdq.math :as math]
            [cdq.projectile :as projectile]
            [cdq.timer :as timer]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :as utils :refer [sort-by-order
                                         pretty-pst
                                         safe-merge]]
            [cdq.vector2 :as v]


            [gdl.application]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.math]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]

            [reduce-fsm :as fsm]))

(extend-type gdl.application.Context
  g/PlayerMovementInput
  (player-movement-vector [ctx]
    (let [r (when (c/key-pressed? ctx :d) [1  0])
          l (when (c/key-pressed? ctx :a) [-1 0])
          u (when (c/key-pressed? ctx :w) [0  1])
          d (when (c/key-pressed? ctx :s) [0 -1])]
      (when (or r l u d)
        (let [v (v/add-vs (remove nil? [r l u d]))]
          (when (pos? (v/length v))
            v))))))

(defmulti ^:private on-clicked
  (fn [ctx eid]
    (:type (:entity/clickable @eid))))

(defmethod on-clicked :clickable/item [{:keys [ctx/player-eid]
                                        :as ctx}
                                       eid]
  (let [item (:entity/item @eid)]
    (cond
     (-> (c/get-actor ctx :windows) :inventory-window ui/visible?)
     [[:tx/sound "bfxr_takeit"]
      [:tx/mark-destroyed eid]
      [:tx/event player-eid :pickup-item item]]

     (inventory/can-pickup-item? (:entity/inventory @player-eid) item)
     [[:tx/sound "bfxr_pickup"]
      [:tx/mark-destroyed eid]
      [:tx/pickup-item player-eid item]]

     :else
     [[:tx/sound "bfxr_denied"]
      [:tx/show-message "Your Inventory is full"]])))

(defmethod on-clicked :clickable/player [_ctx _eid]
  [[:tx/toggle-inventory-visible]]) ; TODO every 'transaction' should have a sound or effect with it?

(defn- clickable->cursor [entity too-far-away?]
  (case (:type (:entity/clickable entity))
    :clickable/item (if too-far-away?
                      :cursors/hand-before-grab-gray
                      :cursors/hand-before-grab)
    :clickable/player :cursors/bag))

(defn- clickable-entity-interaction [ctx player-entity clicked-eid]
  (if (< (v/distance (entity/position player-entity)
                     (entity/position @clicked-eid))
         (:entity/click-distance-tiles player-entity))
    [(clickable->cursor @clicked-eid false) (on-clicked ctx clicked-eid)]
    [(clickable->cursor @clicked-eid true)  [[:tx/sound "bfxr_denied"]
                                             [:tx/show-message "Too far away"]]]))

(defn- mouseover-actor->cursor [actor player-entity-inventory]
  (let [inventory-slot (cdq.ui.inventory/cell-with-item? actor)]
    (cond
     (and inventory-slot
         (get-in player-entity-inventory inventory-slot)) :cursors/hand-before-grab
     (ui/window-title-bar? actor) :cursors/move-window
     (ui/button? actor) :cursors/over-button
     :else :cursors/default)))

(extend-type gdl.application.Context
  g/InteractionState
  (interaction-state [{:keys [ctx/mouseover-eid]
                       :as ctx}
                      eid]
    (let [entity @eid
          mouseover-actor (c/mouseover-actor ctx)]
      (cond
       mouseover-actor
       [(mouseover-actor->cursor mouseover-actor (:entity/inventory entity))
        nil] ; handled by actors themself, they check player state

       (and mouseover-eid
            (:entity/clickable @mouseover-eid))
       (clickable-entity-interaction ctx entity mouseover-eid)

       :else
       (if-let [skill-id (g/selected-skill ctx)]
         (let [skill (skill-id (:entity/skills entity))
               effect-ctx (g/player-effect-ctx ctx eid)
               state (entity/skill-usable-state entity skill effect-ctx)]
           (if (= state :usable)
             ; TODO cursor AS OF SKILL effect (SWORD !) / show already what the effect would do ? e.g. if it would kill highlight
             ; different color ?
             ; => e.g. meditation no TARGET .. etc.
             [:cursors/use-skill
              [[:tx/event eid :start-action [skill effect-ctx]]]]
             ; TODO cursor as of usable state
             ; cooldown -> sanduhr kleine
             ; not-enough-mana x mit kreis?
             ; invalid-params -> depends on params ...
             [:cursors/skill-not-usable
              [[:tx/sound "bfxr_denied"]
               [:tx/show-message (case state
                                   :cooldown "Skill is still on cooldown"
                                   :not-enough-mana "Not enough mana"
                                   :invalid-params "Cannot use this here")]]]))
         [:cursors/no-skill-selected
          [[:tx/sound "bfxr_denied"]
           [:tx/show-message "No selected skill"]]])))))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [ctx position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (c/camera-position ctx)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (c/world-viewport-width ctx))  2)))
     (<= ydist (inc (/ (float (c/world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type gdl.application.Context
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [ctx source target]
    (and (or (not (:entity/player? source))
             (on-screen? ctx (entity/position target)))
         (not (and los-checks?
                   (g/ray-blocked? ctx
                                   (entity/position source)
                                   (entity/position target)))))))

(extend-type gdl.application.Context
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/mouseover-eid] :as ctx}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (entity/position @mouseover-eid))
                              (c/world-mouse-position ctx))]
      {:effect/source eid
       :effect/target mouseover-eid
       :effect/target-position target-position
       :effect/target-direction (v/direction (entity/position @eid) target-position)}))

  (npc-effect-ctx [ctx eid]
    (let [entity @eid
          target (g/nearest-enemy ctx entity)
          target (when (and target
                            (g/line-of-sight? ctx entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (entity/position entity)
                                               (entity/position @target)))})))

; !!! Entity logic/data schema is _all over_ the application !!!

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defn- pay-mana-cost [entity cost]
  (let [mana-val ((entity/mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (position [_]
    position)

  (rectangle [_]
    (let [[x y] left-bottom]
      (gdl.math/rectangle x y width height)))

  (overlaps? [this other-entity]
    (gdl.math/overlaps? (entity/rectangle this)
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

     (not-enough-mana? entity skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable))
  )

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

(defn- context-entity-moved! [{:keys [ctx/content-grid
                                      ctx/grid]}
                              eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))

(defn- context-entity-remove! [{:keys [ctx/entity-ids]} eid]
  (let [id (entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! eid))

(def ^:private components-schema
  (m/schema [:map {:closed true}
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
             [:entity/hp {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/movement-speed {:optional true} :some]
             [:entity/aggro-range {:optional true} :some]
             [:entity/reaction-time {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:entity/mana {:optional true} :some]
             [:entity/strength     {:optional true} :some]
             [:entity/cast-speed   {:optional true} :some]
             [:entity/attack-speed {:optional true} :some]
             [:entity/armor-save   {:optional true} :some]
             [:entity/armor-pierce {:optional true} :some]
             [:entity/modifiers    {:optional true} :some]
             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(defn- spawn-entity! [{:keys [ctx/id-counter] :as ctx}
                      position
                      body
                      components]
  (m/validate-humanize components-schema components)
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      (create-body ctx/minimum-size ctx/z-orders)
                      (utils/safe-merge (-> components
                                            (assoc :entity/id (swap! id-counter inc))
                                            (create-vs ctx)))))]
    (context-entity-add! ctx eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/create! component eid ctx)))
    eid))

(defn- move-entity! [ctx eid body direction rotate-in-movement-direction?]
  (context-entity-moved! ctx eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))

(defn- spawn-effect! [ctx position components]
  (spawn-entity! ctx
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
    (spawn-entity! ctx
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

(defn- create-actors [ctx]
  [((requiring-resolve 'cdq.ui.dev-menu/create) ctx)
   (action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (c/ui-viewport-width ctx) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(c/ui-viewport-width ctx) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(c/ui-viewport-width ctx)
                                                                       (c/ui-viewport-height ctx)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

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
  (spawn-entity! ctx
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
  (swap! eid pay-mana-cost cost))

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

(defn- create-game-state [ctx]
  (c/reset-actors! ctx (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((requiring-resolve (g/config ctx :world-fn)) ctx)
        grid (grid/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (cdq.content-grid/create tiled-map (g/config ctx :content-grid-cell-size))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))

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

(def ^:private explored-tile-color (graphics/color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color graphics/black)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? graphics/white base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              graphics/white))))))

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- draw-world-map! [{:keys [ctx/tiled-map
                                ctx/raycaster
                                ctx/explored-tile-corners]
                         :as ctx}]
  (c/draw-tiled-map! ctx
                     tiled-map
                     (tile-color-setter raycaster
                                        explored-tile-corners
                                        (c/camera-position ctx))))

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
    (grid position))

  (point->entities [{:keys [ctx/grid]} position]
    (grid/point->entities grid position))

  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->cells [{:keys [ctx/grid]} circle]
    (grid/circle->cells grid circle))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(defn- remove-destroyed-entities! [{:keys [ctx/entity-ids] :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (context-entity-remove! ctx eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/destroy! component eid ctx))))
  nil)

(extend-type gdl.application.Context
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type gdl.application.Context
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (filter #(= "properties" (namespace %)) (keys (:schemas db))))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [{:keys [ctx/db] :as ctx}
                     property]
    (let [new-db (db/update db property)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db)))

  (delete-property! [{:keys [ctx/db] :as ctx}
                     property-id]
    (let [new-db (db/delete db property-id)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db))))

(defn- geom-test* [ctx]
  (let [position (c/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (g/circle->cells ctx circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [ctx]
  (c/handle-draws! ctx (geom-test* ctx)))

(defn- highlight-mouseover-tile* [ctx]
  (let [[x y] (mapv int (c/world-mouse-position ctx))
        cell (g/grid-cell ctx [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [ctx]
  (c/handle-draws! ctx (highlight-mouseover-tile* ctx)))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- draw-tile-grid* [ctx]
  (when ctx/show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (c/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (c/world-viewport-width ctx)))
        (+ 2 (int (c/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-tile-grid [ctx]
  (c/handle-draws! ctx (draw-tile-grid* ctx)))

(defn- draw-cell-debug* [ctx]
  (apply concat
         (for [[x y] (c/visible-tiles ctx)
               :let [cell (g/grid-cell ctx [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and ctx/show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction ctx/show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (ctx/factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- draw-cell-debug [ctx]
  (c/handle-draws! ctx (draw-cell-debug* ctx)))

(defn- render-entities! [{:keys [ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              ctx/render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (c/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (c/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (c/handle-draws! ctx (draw-body-rect entity :red))
         (pretty-pst t))))))

(defn- camera-controls! [ctx]
  (let [controls (g/config ctx :controls)
        zoom-speed (g/config ctx :zoom-speed)]
    (when (c/key-pressed? ctx (:zoom-in controls))  (c/inc-zoom! ctx    zoom-speed))
    (when (c/key-pressed? ctx (:zoom-out controls)) (c/inc-zoom! ctx (- zoom-speed)))))

(defn- get-active-entities [{:keys [ctx/content-grid
                                    ctx/player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn- player-state-handle-click! [{:keys [ctx/player-eid] :as ctx}]
  (g/handle-txs! ctx
                 (state/manual-tick (entity/state-obj @player-eid)
                                    player-eid
                                    ctx))
  nil)

(defn- update-mouseover-entity! [{:keys [ctx/player-eid
                                         ctx/mouseover-eid]
                                  :as ctx}]
  (let [new-eid (if (c/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (g/point->entities ctx (c/world-mouse-position ctx)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(g/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(defn- pause-game? [{:keys [ctx/player-eid] :as ctx}]
  (let [controls (g/config ctx :controls)]
    (or #_error
        (and (g/config ctx :pausing?)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (c/key-just-pressed? ctx (:unpause-once controls))
                      (c/key-pressed? ctx (:unpause-continously controls))))))))

(defn- assoc-paused [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))

(defn- tick-entities!
  [{:keys [ctx/active-entities] :as ctx}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (g/handle-txs! ctx (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info (str "entity/id: " (entity/id @eid)) {} t)))))
   (catch Throwable t
     (pretty-pst t)
     (g/open-error-window! ctx t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- assoc-delta-time
  [ctx]
  (assoc ctx :ctx/delta-time (min (c/delta-time ctx) ctx/max-delta)))

(defn- update-elapsed-time
  [{:keys [ctx/delta-time]
    :as ctx}]
  (update ctx :ctx/elapsed-time + delta-time))

(defn- render-game-state! [{:keys [ctx/player-eid] :as ctx}]
  (ctx-schema/validate ctx)
  (let [ctx (assoc ctx :ctx/active-entities (get-active-entities ctx))]
    (c/set-camera-position! ctx (entity/position @player-eid))
    (c/clear-screen! ctx)
    (draw-world-map! ctx)
    (c/draw-on-world-viewport! ctx [draw-tile-grid
                                    draw-cell-debug
                                    render-entities!
                                    ;geom-test
                                    highlight-mouseover-tile])
    (c/draw-stage! ctx)
    (c/update-stage! ctx)
    (player-state-handle-click! ctx)
    (let [ctx (update-mouseover-entity! ctx)
          ctx (assoc-paused ctx)
          ctx (if (:ctx/paused? ctx)
                ctx
                (let [ctx (-> ctx
                              assoc-delta-time
                              update-elapsed-time)]
                  (potential-fields.update/do! ctx)
                  (tick-entities! ctx)
                  ctx))]
      (remove-destroyed-entities! ctx) ; do not pause as pickup item should be destroyed
      (camera-controls! ctx)
      ctx)))

(defn reset-game-state! []
  (swap! gdl.application/state create-game-state))

(defn -main []
  (let [config (config/create "config.edn")]
    (run! require (:requires config))
    (gdl.application/start! config
                            (fn [context]
                              (-> context
                                  (safe-merge {:ctx/config config
                                               :ctx/db (db/create (:db config))})
                                  create-game-state))
                            render-game-state!

                              #_(dispose! [_]
                                ; nil
                                ; TODO dispose world tiled-map/level resources?
                                )
                              )))
