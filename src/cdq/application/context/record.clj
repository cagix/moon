(ns cdq.application.context.record
  (:require [cdq.op :as op]
            [cdq.db-impl]
            cdq.draw-impl
            cdq.draw-on-world-viewport.tile-grid
            cdq.draw-on-world-viewport.cell-debug
            cdq.draw-on-world-viewport.entities
            cdq.draw-on-world-viewport.geom-test
            cdq.draw-on-world-viewport.highlight-mouseover-tile
            [cdq.animation :as animation]
            [cdq.audio :as audio]
            [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.controls :as controls]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.body :as body]
            cdq.entity-tick
            [cdq.entity.state :as state]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.entity.state.player-idle]
            [cdq.faction :as faction]
            [cdq.gdx.math.vector2 :as v]
            [cdq.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.image :as image]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.malli :as m]
            [cdq.game.effects]
            [cdq.editor-widgets]
            [cdq.rand :refer [rand-int-between]]
            [cdq.raycaster :as raycaster]
            [cdq.stage]
            [cdq.skill :as skill]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.message]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.utils :as utils :refer [find-first]]
            [cdq.val-max :as val-max]
            [cdq.world :as world]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.button :as button]
            [clojure.math :as math]
            [clojure.string :as str]
            [clojure.vis-ui.widget :as widget]
            [clojure.vis-ui.window :as window]
            [qrecord.core :as q]
            [reduce-fsm :as fsm]))

(defmacro def-record-and-schema [record-sym & ks]
  `(do

    (q/defrecord ~record-sym
      ~(mapv (comp symbol first) ks))

    (def schema
      [:map {:closed true} ~@ks])

    ))

(def-record-and-schema Context
  [:ctx/active-entities :some]
  [:ctx/audio :some]
  [:ctx/batch :some]
  [:ctx/config :some]
  [:ctx/content-grid :some]
  [:ctx/cursors :some]
  [:ctx/db :some]
  [:ctx/default-font :some]
  [:ctx/delta-time :some]
  [:ctx/draw-fns :some]
  [:ctx/draw-on-world-viewport :some]
  [:ctx/elapsed-time :some]
  [:ctx/entity-ids :some]
  [:ctx/explored-tile-corners :some]
  [:ctx/factions-iterations :some]
  [:ctx/graphics :some]
  [:ctx/grid :some]
  [:ctx/id-counter :some]
  [:ctx/info :some]
  [:ctx/input :some]
  [:ctx/max-delta :some]
  [:ctx/max-speed :some]
  [:ctx/minimum-size :some]
  [:ctx/mouseover-eid :any]
  [:ctx/paused? :any]
  [:ctx/player-eid :some]
  [:ctx/potential-field-cache :some]
  [:ctx/raycaster :some]
  [:ctx/render-layers :some]
  [:ctx/render-z-order :some]
  [:ctx/schema :some]
  [:ctx/shape-drawer :some]
  [:ctx/shape-drawer-texture :some]
  [:ctx/stage :some]
  [:ctx/textures :some]
  [:ctx/tiled-map :some]
  [:ctx/tiled-map-renderer :some]
  [:ctx/ui-actors :some]
  [:ctx/ui-viewport :some]
  [:ctx/unit-scale :some]
  [:ctx/world-unit-scale :some]
  [:ctx/world-viewport :some]
  [:ctx/z-orders :some]

  [:cdq.application.start/pipeline :some]
  [:ctx/os-settings :some]
  [:ctx/lwjgl :some]
  [:ctx/create-fn :some]
  [:ctx/dispose-fn :some]
  [:ctx/resize-fn :some]
  [:ctx/application-state :some]
  [:ctx/fsms :some]
  [:ctx/entity-components :some]
  [:ctx/spawn-entity-schema :some]
  [:ctx/render-fn :some]
  [:ctx/reset-game-state-fn :some]
  )

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

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn- show-modal-window! [stage
                           ui-viewport
                           {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (stage/add! stage
              (widget/window {:title title
                              :rows [[{:actor {:actor/type :actor.type/label
                                               :label/text text}}]
                                     [(widget/text-button button-text
                                                          (fn [_actor _ctx]
                                                            (actor/remove! (::modal stage))
                                                            (on-click)))]]
                              :id ::modal
                              :modal? true
                              :center-position [(/ (:viewport/width  ui-viewport) 2)
                                                (* (:viewport/height ui-viewport) (/ 3 4))]
                              :pack? true})))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/remove-item! inventory-cell)))

(defn- set-item!
  [{:keys [ctx/textures
           ctx/stage]
    :as ctx}
   inventory-cell item]
  (-> stage
      :windows
      :inventory-window
      (inventory-window/set-item! inventory-cell
                                  {:texture-region (image/texture-region (:entity/image item) textures)
                                   :tooltip-text (fn [ctx]
                                                   (info/generate (:ctx/info ctx) item ctx))})))

(defn- add-skill!
  [{:keys [ctx/textures
           ctx/stage]}
   skill]
  (-> stage
      :action-bar
      (action-bar/add-skill! {:skill-id (:property/id skill)
                              :texture-region (image/texture-region (:entity/image skill) textures)
                              :tooltip-text (fn [ctx]
                                              (info/generate (:ctx/info ctx) skill ctx))}))
  nil)

#_(defn- remove-skill! [{:keys [ctx/stage]} skill]
    (-> stage
        :action-bar
        (action-bar/remove-skill! (:property/id skill)))
    nil)

(defn- add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? (:entity/skills entity) id))]}
  (assoc-in entity [:entity/skills id] skill))

(defn- add-text-effect [entity text duration elapsed-time]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter timer/increment duration))
           {:text text
            :counter (timer/create elapsed-time duration)})))

(defn- pay-mana-cost [entity cost]
  (update entity :creature/stats stats/pay-mana-cost cost))

(declare txs-fn-map)

(defn do!
  [{k 0 :as component}
   ctx]
  ((get txs-fn-map k) component ctx))

(def txs-fn-map
  {:tx/assoc (fn [[_ eid k value] _ctx]
               (swap! eid assoc k value)
               nil)

   :tx/assoc-in (fn [[_ eid ks value] _ctx]
                  (swap! eid assoc-in ks value)
                  nil)

   :tx/dissoc (fn [[_ eid k] _ctx]
                (swap! eid dissoc k)
                nil)

   :tx/effect (fn [[_ effect-ctx effects] ctx]
                (mapcat #(effect/handle % effect-ctx ctx)
                        (effect/filter-applicable? effect-ctx effects)))

   :tx/mark-destroyed (fn [[_ eid] _ctx]
                        (swap! eid assoc :entity/destroyed? true)
                        nil)

   :tx/mod-add (fn [[_ eid modifiers] _ctx]
                 (swap! eid update :creature/stats stats/add modifiers)
                 nil)

   :tx/mod-remove (fn [[_ eid modifiers] _ctx]
                    (swap! eid update :creature/stats stats/remove modifiers)
                    nil)

   :tx/pay-mana-cost (fn [[_ eid cost] _ctx]
                       (swap! eid pay-mana-cost cost)
                       nil)

   :tx/pickup-item (fn [[_ eid item] ctx]
                     (inventory/assert-valid-item? item)
                     (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
                       (assert cell)
                       (assert (or (inventory/stackable? item cell-item)
                                   (nil? cell-item)))
                       (if (inventory/stackable? item cell-item)
                         (do
                          #_(tx/stack-item ctx eid cell item))
                         (do! [:tx/set-item eid cell item] ctx))))

   :tx/set-cooldown (fn [[_ eid skill] {:keys [ctx/elapsed-time]}]
                      (swap! eid assoc-in
                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                             (timer/create elapsed-time (:skill/cooldown skill)))
                      nil)

   :tx/add-text-effect (fn [[_ eid text duration] {:keys [ctx/elapsed-time]}]
                         (swap! eid add-text-effect text duration elapsed-time)
                         nil)

   :tx/add-skill (fn [[_ eid skill] ctx]
                   (swap! eid add-skill skill)
                   (when (:entity/player? @eid)
                     (add-skill! ctx skill))
                   nil)

   #_(defn remove-skill [eid {:keys [property/id] :as skill}]
       {:pre [(contains? (:entity/skills @eid) id)]}
       (swap! eid update :entity/skills dissoc id)
       (when (:entity/player? @eid)
         (remove-skill! ctx skill)))

   :tx/set-item (fn [[_ eid cell item] ctx]
                  (let [entity @eid
                        inventory (:entity/inventory entity)]
                    (assert (and (nil? (get-in inventory cell))
                                 (inventory/valid-slot? cell item)))
                    (swap! eid assoc-in (cons :entity/inventory cell) item)
                    (when (inventory/applies-modifiers? cell)
                      (swap! eid update :creature/stats stats/add (:entity/modifiers item)))
                    (when (:entity/player? entity)
                      (set-item! ctx cell item))
                    nil))

   :tx/remove-item (fn [[_ eid cell] ctx]
                     (let [entity @eid
                           item (get-in (:entity/inventory entity) cell)]
                       (assert item)
                       (swap! eid assoc-in (cons :entity/inventory cell) nil)
                       (when (inventory/applies-modifiers? cell)
                         (swap! eid update :creature/stats stats/remove (:entity/modifiers item)))
                       (when (:entity/player? entity)
                         (remove-item! ctx cell))
                       nil))

   :tx/event (fn [[_ eid event params] ctx]
               (let [fsm (:entity/fsm @eid)
                     _ (assert fsm)
                     old-state-k (:state fsm)
                     new-fsm (fsm/fsm-event fsm event)
                     new-state-k (:state new-fsm)]
                 (when-not (= old-state-k new-state-k)
                   (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                                         [k (k @eid)])
                         new-state-obj [new-state-k (state/create ctx new-state-k eid params)]]
                     [[:tx/assoc eid :entity/fsm new-fsm]
                      [:tx/assoc eid new-state-k (new-state-obj 1)]
                      [:tx/dissoc eid old-state-k]
                      [:tx/state-exit eid old-state-obj]
                      [:tx/state-enter eid new-state-obj]]))))

   :tx/toggle-inventory-visible (fn [_ ctx]
                                  (-> ctx
                                      :ctx/stage
                                      cdq.stage/toggle-inventory-visible!)
                                  nil)
   :tx/show-message (fn
                      [[_ message]
                       {:keys [ctx/stage]}]
                      (-> stage
                          stage/root
                          (group/find-actor "player-message")
                          (cdq.ui.message/show! message))
                      nil)

   :tx/show-modal (fn [[_ opts] {:keys [ctx/stage
                                        ctx/ui-viewport]}]
                    (show-modal-window! stage
                                        ui-viewport
                                        opts)
                    nil)

   :tx/sound (fn [[_ sound-name] {:keys [ctx/audio]}]
               (audio/play-sound! audio sound-name)
               nil)

   :tx/state-exit (fn [[_ eid [state-k state-v]] ctx]
                    (when-let [f (state-k state/state->exit)]
                      (f state-v eid ctx)))

   :tx/state-enter (fn [[_ eid [state-k state-v]] _ctx]
                     (when-let [f (state-k state/state->enter)]
                       (f state-v eid)))

   :tx/audiovisual (fn [[_ position audiovisual]
                        {:keys [ctx/db]}]
                     (let [{:keys [tx/sound
                                   entity/animation]} (if (keyword? audiovisual)
                                                        (db/build db audiovisual)
                                                        audiovisual)]
                       [[:tx/sound sound]
                        [:tx/spawn-effect
                         position
                         {:entity/animation animation
                          :entity/delete-after-animation-stopped? true}]]))

   :tx/spawn-alert (fn [[_ position faction duration]
                        {:keys [ctx/elapsed-time]}]
                     [[:tx/spawn-effect
                       position
                       {:entity/alert-friendlies-after-duration
                        {:counter (timer/create elapsed-time duration)
                         :faction faction}}]])

   :tx/spawn-line (fn
                    [[_ {:keys [start end duration color thick?]}]
                     _ctx]
                    [[:tx/spawn-effect
                      start
                      {:entity/line-render {:thick? thick? :end end :color color}
                       :entity/delete-after-duration duration}]])

   :tx/deal-damage (fn
                     [[_ source target damage] _ctx]
                     (let [source* @source
                           target* @target
                           hp (stats/get-hitpoints (:creature/stats target*))]
                       (cond
                        (zero? (hp 0))
                        nil

                        (< (rand) (stats/effective-armor-save (:creature/stats source*)
                                                              (:creature/stats target*)))
                        [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

                        :else
                        (let [min-max (:damage/min-max (stats/damage (:creature/stats source*)
                                                                     (:creature/stats target*)
                                                                     damage))
                              dmg-amount (rand-int-between min-max)
                              new-hp-val (max (- (hp 0) dmg-amount)
                                              0)]
                          [[:tx/assoc-in target [:creature/stats :entity/hp 0] new-hp-val]
                           [:tx/event    target (if (zero? new-hp-val) :kill :alert)]
                           [:tx/audiovisual (entity/position target*) :audiovisuals/damage]
                           [:tx/add-text-effect target (str "[RED]" dmg-amount "[]") 0.3]]))))

   :tx/move-entity (fn
                     [[_ eid body direction rotate-in-movement-direction?]
                      {:keys [ctx/content-grid
                              ctx/grid]}]
                     (content-grid/position-changed! content-grid eid)
                     (grid/position-changed! grid eid)
                     (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
                     (when rotate-in-movement-direction?
                       (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
                     nil)

   :tx/spawn-projectile (fn
                          [[_
                            {:keys [position direction faction]}
                            {:keys [entity/image
                                    projectile/max-range
                                    projectile/speed
                                    entity-effects
                                    projectile/size
                                    projectile/piercing?] :as projectile}]
                           _ctx]
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

   :tx/spawn-effect (fn [[_ position components]
                         {:keys [ctx/config]}]
                      [[:tx/spawn-entity
                        (assoc components
                               :entity/body (assoc (:effect-body-props config) :position position))]])

   :tx/spawn-item (fn [[_ position item]
                       _ctx]
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
   :tx/spawn-creature (fn
                        [[_ {:keys [position
                                    creature-property
                                    components]}]
                         _ctx]
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

   :tx/spawn-entity (fn [[_ components]
                         {:keys [ctx/id-counter
                                 ctx/entity-ids
                                 ctx/entity-components
                                 ctx/spawn-entity-schema
                                 ctx/content-grid
                                 ctx/grid]
                          :as ctx}]
                      (m/validate-humanize spawn-entity-schema components)
                      (assert (and (not (contains? components :entity/id))))
                      (let [eid (atom (merge (world/map->Entity {})
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
                                @eid)))})

(defn- valid-tx? [transaction]
  (vector? transaction))

(defn- handle-tx! [tx ctx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do! tx ctx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

(extend-type Context
  cdq.ctx/TransactionHandler
  (handle-txs! [ctx transactions]
    (loop [ctx ctx
           txs transactions
           handled []]
      (if (seq txs)
        (let [tx (first txs)]
          (if tx
            (let [new-txs (handle-tx! tx ctx)]
              (recur ctx
                     (concat (or new-txs []) (rest txs))
                     (conj handled tx)))
            (recur ctx (rest txs) handled)))
        handled))))

(def reaction-time-multiplier 0.016)

(defn- apply-action-speed-modifier [{:keys [creature/stats]} skill action-time]
  (/ action-time
     (or (stats/get-stat-value stats (:skill/action-time-modifier-key skill))
         1)))

(def state->create
  {:active-skill          (fn [eid [skill effect-ctx] {:keys [ctx/elapsed-time]}]
                            {:skill skill
                             :effect-ctx effect-ctx
                             :counter (->> skill
                                           :skill/action-time
                                           (apply-action-speed-modifier @eid skill)
                                           (timer/create elapsed-time))})
   :npc-moving            (fn [eid movement-vector {:keys [ctx/elapsed-time]}]
                            {:movement-vector movement-vector
                             :timer (timer/create elapsed-time
                                                  (* (stats/get-stat-value (:creature/stats @eid) :entity/reaction-time)
                                                     reaction-time-multiplier))})
   :player-item-on-cursor (fn [_eid item _ctx]
                            {:item item})
   :player-moving         (fn [eid movement-vector _ctx]
                            {:movement-vector movement-vector})
   :stunned               (fn [_eid duration {:keys [ctx/elapsed-time]}]
                            {:counter (timer/create elapsed-time duration)})})

(def state->enter {:npc-dead              (fn [_ eid]
                                     [[:tx/mark-destroyed eid]])
            :npc-moving            (fn [{:keys [movement-vector]} eid]
                                     [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                       :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                                  0)}]])
            :player-dead           (fn [_ _eid]
                                     [[:tx/sound "bfxr_playerdeath"]
                                      [:tx/show-modal {:title "YOU DIED - again!"
                                                       :text "Good luck next time!"
                                                       :button-text "OK"
                                                       :on-click (fn [])}]])
            :player-item-on-cursor (fn [{:keys [item]} eid]
                                     [[:tx/assoc eid :entity/item-on-cursor item]])
            :player-moving         (fn [{:keys [movement-vector]} eid]
                                     [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                       :speed (or (stats/get-stat-value (:creature/stats @eid) :entity/movement-speed)
                                                                                  0)}]])
            :active-skill          (fn [{:keys [skill]} eid]
                                     [[:tx/sound (:skill/start-action-sound skill)]
                                      (when (:skill/cooldown skill)
                                        [:tx/set-cooldown eid skill])
                                      (when (and (:skill/cost skill)
                                                 (not (zero? (:skill/cost skill))))
                                        [:tx/pay-mana-cost eid (:skill/cost skill)])])})

(def state->exit
  {:npc-moving            (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])
   :npc-sleeping          (fn [_ eid _ctx]
                            [[:tx/spawn-alert (entity/position @eid) (:entity/faction @eid) 0.2]
                             [:tx/add-text-effect eid "[WHITE]!" 1]])
   :player-item-on-cursor (fn [_ eid {:keys [ctx/world-mouse-position]}]
                            ; at clicked-cell when we put it into a inventory-cell
                            ; we do not want to drop it on the ground too additonally,
                            ; so we dissoc it there manually. Otherwise it creates another item
                            ; on the ground
                            (let [entity @eid]
                              (when (:entity/item-on-cursor entity)
                                [[:tx/sound "bfxr_itemputground"]
                                 [:tx/dissoc eid :entity/item-on-cursor]
                                 [:tx/spawn-item
                                  (cdq.entity.state.player-item-on-cursor/item-place-position world-mouse-position entity)
                                  (:entity/item-on-cursor entity)]])))
   :player-moving         (fn [_ eid _ctx]
                            [[:tx/dissoc eid :entity/movement]])})

(def state->cursor
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle (fn [player-eid ctx]
                  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
                    (case k
                      :interaction-state/mouseover-actor
                      (let [actor params]
                        (let [inventory-slot (inventory-window/cell-with-item? actor)]
                          (cond
                           (and inventory-slot
                                (get-in (:entity/inventory @player-eid) inventory-slot)) :cursors/hand-before-grab
                           (window/title-bar? actor) :cursors/move-window
                           (button/is?        actor) :cursors/over-button
                           :else :cursors/default)))

                      :interaction-state/clickable-mouseover-eid
                      (let [{:keys [clicked-eid
                                    in-click-range?]} params]
                        (case (:type (:entity/clickable @clicked-eid))
                          :clickable/item (if in-click-range?
                                            :cursors/hand-before-grab
                                            :cursors/hand-before-grab-gray)
                          :clickable/player :cursors/bag))

                      :interaction-state.skill/usable
                      :cursors/use-skill

                      :interaction-state.skill/not-usable
                      :cursors/skill-not-usable

                      :interaction-state/no-skill-selected
                      :cursors/no-skill-selected)))
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})

(defn inventory-window-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn can-pickup-item? [entity item]
  (inventory/can-pickup-item? (:entity/inventory entity) item))

(defn interaction-state->txs [ctx player-eid]
  (let [[k params] (cdq.entity.state.player-idle/interaction-state ctx player-eid)]
    (case k
      :interaction-state/mouseover-actor nil ; handled by ui actors themself.

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (if in-click-range?
          (case (:type (:entity/clickable @clicked-eid))
            :clickable/player
            [[:tx/toggle-inventory-visible]]

            :clickable/item
            (let [item (:entity/item @clicked-eid)]
              (cond
               (inventory-window-visible? (:ctx/stage ctx))
               [[:tx/sound "bfxr_takeit"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/event player-eid :pickup-item item]]

               (can-pickup-item? @player-eid item)
               [[:tx/sound "bfxr_pickup"]
                [:tx/mark-destroyed clicked-eid]
                [:tx/pickup-item player-eid item]]

               :else
               [[:tx/sound "bfxr_denied"]
                [:tx/show-message "Your Inventory is full"]])))
          [[:tx/sound "bfxr_denied"]
           [:tx/show-message "Too far away"]]))

      :interaction-state.skill/usable
      (let [[skill effect-ctx] params]
        [[:tx/event player-eid :start-action [skill effect-ctx]]])

      :interaction-state.skill/not-usable
      (let [state params]
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message (case state
                             :cooldown "Skill is still on cooldown"
                             :not-enough-mana "Not enough mana"
                             :invalid-params "Cannot use this here")]])

      :interaction-state/no-skill-selected
      [[:tx/sound "bfxr_denied"]
       [:tx/show-message "No selected skill"]])))

(defn- speed [{:keys [creature/stats]}]
  (or (stats/get-stat-value stats :entity/movement-speed)
      0))

(def state->handle-input
  {:player-idle           (fn [player-eid {:keys [ctx/input] :as ctx}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/event player-eid :movement-input movement-vector]]
                              (when (input/button-just-pressed? input :left)
                                (interaction-state->txs ctx player-eid))))

   :player-item-on-cursor (fn [eid
                               {:keys [ctx/input
                                       ctx/mouseover-actor]}]
                            (when (and (input/button-just-pressed? input :left)
                                       (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor))
                              [[:tx/event eid :drop-item]]))
   :player-moving         (fn [eid {:keys [ctx/input]}]
                            (if-let [movement-vector (controls/player-movement-vector input)]
                              [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                :speed (speed @eid)}]]
                              [[:tx/event eid :no-movement-input]]))})

(defn- move-position [position {:keys [direction speed delta-time]}]
  (mapv #(+ %1 (* %2 speed delta-time)) position direction))

(defn- move-body [body movement]
  (update body :body/position move-position movement))

(defn- try-move [grid body entity-id movement]
  (let [new-body (move-body body movement)]
    (when (grid/valid-position? grid new-body entity-id)
      new-body)))

; TODO sliding threshold
; TODO name - with-sliding? 'on'
; TODO if direction was [-1 0] and invalid-position then this algorithm tried to move with
; direection [0 0] which is a waste of processor power...
(defn- try-move-solid-body [grid body entity-id {[vx vy] :direction :as movement}]
  (let [xdir (Math/signum (float vx))
        ydir (Math/signum (float vy))]
    (or (try-move grid body entity-id movement)
        (try-move grid body entity-id (assoc movement :direction [xdir 0]))
        (try-move grid body entity-id (assoc movement :direction [0 ydir])))))

; this is not necessary if effect does not need target, but so far not other solution came up.
(defn- update-effect-ctx
  "Call this on effect-context if the time of using the context is not the time when context was built."
  [raycaster {:keys [effect/source effect/target] :as effect-ctx}]
  (if (and target
           (not (:entity/destroyed? @target))
           (raycaster/line-of-sight? raycaster @source @target))
    effect-ctx
    (dissoc effect-ctx :effect/target)))

(defn- npc-effect-ctx
  [{:keys [ctx/raycaster
           ctx/grid]}
   eid]
  (let [entity @eid
        target (grid/nearest-enemy grid entity)
        target (when (and target
                          (raycaster/line-of-sight? raycaster entity @target))
                 target)]
    {:effect/source eid
     :effect/target target
     :effect/target-direction (when target
                                (v/direction (entity/position entity)
                                             (entity/position @target)))}))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (skill/usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(def entity->tick
  {:entity/alert-friendlies-after-duration (fn [{:keys [counter faction]}
                                                eid
                                                {:keys [ctx/elapsed-time
                                                        ctx/grid]}]
                                             (when (timer/stopped? elapsed-time counter)
                                               (cons [:tx/mark-destroyed eid]
                                                     (for [friendly-eid (->> {:position (entity/position @eid)
                                                                              :radius 4}
                                                                             (grid/circle->entities grid)
                                                                             (filter #(= (:entity/faction @%) faction)))]
                                                       [:tx/event friendly-eid :alert]))))
   :entity/animation (fn [animation eid {:keys [ctx/delta-time]}]
                       [[:tx/assoc eid :entity/animation (animation/tick animation delta-time)]])
   :entity/delete-after-animation-stopped? (fn [_ eid _ctx]
                                             (when (animation/stopped? (:entity/animation @eid))
                                               [[:tx/mark-destroyed eid]]))
   :entity/delete-after-duration (fn [counter eid {:keys [ctx/elapsed-time]}]
                                   (when (timer/stopped? elapsed-time counter)
                                     [[:tx/mark-destroyed eid]]))
   :entity/movement (fn [{:keys [direction
                                 speed
                                 rotate-in-movement-direction?]
                          :as movement}
                         eid
                         {:keys [ctx/delta-time
                                 ctx/grid
                                 ctx/max-speed]}]
                      (assert (<= 0 speed max-speed)
                              (pr-str speed))
                      (assert (or (zero? (v/length direction))
                                  (v/nearly-normalised? direction))
                              (str "cannot understand direction: " (pr-str direction)))
                      (when-not (or (zero? (v/length direction))
                                    (nil? speed)
                                    (zero? speed))
                        (let [movement (assoc movement :delta-time delta-time)
                              body (:entity/body @eid)]
                          (when-let [body (if (:body/collides? body) ; < == means this is a movement-type ... which could be a multimethod ....
                                            (try-move-solid-body grid body (:entity/id @eid) movement)
                                            (move-body body movement))]
                            [[:tx/move-entity eid body direction rotate-in-movement-direction?]]))))
   :entity/projectile-collision (fn [{:keys [entity-effects already-hit-bodies piercing?]}
                                     eid
                                     {:keys [ctx/grid]}]
                                  ; TODO this could be called from body on collision
                                  ; for non-solid
                                  ; means non colliding with other entities
                                  ; but still collding with other stuff here ? o.o
                                  (let [entity @eid
                                        cells* (map deref (grid/body->cells grid (:entity/body entity))) ; just use cached-touched -cells
                                        hit-entity (find-first #(and (not (contains? already-hit-bodies %)) ; not filtering out own id
                                                                     (not= (:entity/faction entity) ; this is not clear in the componentname & what if they dont have faction - ??
                                                                           (:entity/faction @%))
                                                                     (:body/collides? (:entity/body @%))
                                                                     (body/overlaps? (:entity/body entity)
                                                                                     (:entity/body @%)))
                                                               (grid/cells->entities grid cells*))
                                        destroy? (or (and hit-entity (not piercing?))
                                                     (some #(cell/blocked? % (:body/z-order (:entity/body entity))) cells*))]
                                    [(when destroy?
                                       [:tx/mark-destroyed eid])
                                     (when hit-entity
                                       [:tx/assoc-in eid [:entity/projectile-collision :already-hit-bodies] (conj already-hit-bodies hit-entity)] ; this is only necessary in case of not piercing ...
                                       )
                                     (when hit-entity
                                       [:tx/effect {:effect/source eid :effect/target hit-entity} entity-effects])]))
   :entity/skills (fn [skills eid {:keys [ctx/elapsed-time]}]
                    (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
                          :when (and cooling-down?
                                     (timer/stopped? elapsed-time cooling-down?))]
                      [:tx/assoc-in eid [:entity/skills (:property/id skill) :skill/cooling-down?] false]))
   :active-skill (fn [{:keys [skill effect-ctx counter]}
                      eid
                      {:keys [ctx/elapsed-time
                              ctx/raycaster]}]
                   (cond
                    (not (effect/some-applicable? (update-effect-ctx raycaster effect-ctx) ; TODO how 2 test
                                                  (:skill/effects skill)))
                    [[:tx/event eid :action-done]
                     ; TODO some sound ?
                     ]

                    (timer/stopped? elapsed-time counter)
                    [[:tx/effect effect-ctx (:skill/effects skill)]
                     [:tx/event eid :action-done]]))
   :npc-idle (fn [_ eid {:keys [ctx/grid] :as ctx}]
               (let [effect-ctx (npc-effect-ctx ctx eid)]
                 (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
                   [[:tx/event eid :start-action [skill effect-ctx]]]
                   [[:tx/event eid :movement-direction (or (potential-fields.movement/find-direction grid eid)
                                                           [0 0])]])))
   :npc-moving (fn [{:keys [timer]} eid {:keys [ctx/elapsed-time]}]
                 (when (timer/stopped? elapsed-time timer)
                   [[:tx/event eid :timer-finished]]))
   :npc-sleeping (fn [_ eid {:keys [ctx/grid]}]
                   (let [entity @eid]
                     (when-let [distance (grid/nearest-enemy-distance grid entity)]
                       (when (<= distance (stats/get-stat-value (:creature/stats entity) :entity/aggro-range))
                         [[:tx/event eid :alert]]))))
   :stunned (fn [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
              (when (timer/stopped? elapsed-time counter)
                [[:tx/event eid :effect-wears-off]]))
   :entity/string-effect (fn [{:keys [counter]} eid {:keys [ctx/elapsed-time]}]
                           (when (timer/stopped? elapsed-time counter)
                             [[:tx/dissoc eid :entity/string-effect]]))
   :entity/temp-modifier (fn [{:keys [modifiers counter]}
                              eid
                              {:keys [ctx/elapsed-time]}]
                           (when (timer/stopped? elapsed-time counter)
                             [[:tx/dissoc eid :entity/temp-modifier]
                              [:tx/mod-remove eid modifiers]]))})

(.bindRoot #'cdq.entity.state/->create state->create)
(.bindRoot #'cdq.entity.state/state->enter state->enter)
(.bindRoot #'cdq.entity.state/state->cursor state->cursor)
(.bindRoot #'cdq.entity.state/state->exit state->exit)
(.bindRoot #'cdq.entity.state/state->handle-input state->handle-input)
(.bindRoot #'cdq.entity-tick/entity->tick entity->tick)




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

(defmulti ^:private op-value-text (fn [[k]]
                                    k))

(defmethod op-value-text :op/inc
  [[_ value]]
  (str value))

(defmethod op-value-text :op/mult
  [[_ value]]
  (str value "%"))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn- op-info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text component) " " (str/capitalize (name k)))))
             (sort-by op/-order op))))

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(def ^:private non-val-max-stat-ks
  [:entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(def info-configuration
  {:k->colors {:property/pretty-name "PRETTY_NAME"
               :entity/modifiers "CYAN"
               :maxrange "LIGHT_GRAY"
               :creature/level "GRAY"
               :projectile/piercing? "LIME"
               :skill/action-time-modifier-key "VIOLET"
               :skill/action-time "GOLD"
               :skill/cooldown "SKY"
               :skill/cost "CYAN"
               :entity/delete-after-duration "LIGHT_GRAY"
               :entity/faction "SLATE"
               :entity/fsm "YELLOW"
               :entity/species "LIGHT_GRAY"
               :entity/temp-modifier "LIGHT_GRAY"}
   :k-order [:property/pretty-name
             :skill/action-time-modifier-key
             :skill/action-time
             :skill/cooldown
             :skill/cost
             :skill/effects
             :entity/species
             :creature/level
             :creature/stats
             :entity/delete-after-duration
             :projectile/piercing?
             :entity/projectile-collision
             :maxrange
             :entity-effects]
   :info-fns {:creature/level (fn [[_ v] _ctx]
                                (str "Level: " v))
              :creature/stats (fn [[k stats] _ctx]
                                (str/join "\n" (concat
                                                ["*STATS*"
                                                 (str "Mana: " (if (:entity/mana stats)
                                                                 (stats/get-mana stats)
                                                                 "-"))
                                                 (str "Hitpoints: " (stats/get-hitpoints stats))]
                                                (for [stat-k non-val-max-stat-ks]
                                                  (str (str/capitalize (name stat-k)) ": "
                                                       (stats/get-stat-value stats stat-k))))))
              :effects.target/convert (fn [_ _ctx]
                                        "Converts target to your side.")
              :effects.target/damage (fn [[_ damage] _ctx]
                                       (damage-info damage)
                                       #_(if source
                                           (let [modified (stats/damage @source damage)]
                                             (if (= damage modified)
                                               (damage-info damage)
                                               (str (damage-info damage) "\nModified: " (damage/info modified))))
                                           (damage-info damage)) ; property menu no source,modifiers
                                       )
              :effects.target/kill (fn [_ _ctx] "Kills target")
              :effects.target/melee-damage (fn [_ _ctx]
                                             (str "Damage based on entity strength."
                                                  #_(when source
                                                      (str "\n" (damage-info (entity->melee-damage @source))))))
              :effects.target/spiderweb (fn [_ _ctx] "Spiderweb slows 50% for 5 seconds.")
              :effects.target/stun (fn [[_ duration] _ctx]
                                     (str "Stuns for " (utils/readable-number duration) " seconds"))
              :effects/spawn (fn [[_ {:keys [property/pretty-name]}] _ctx]
                               (str "Spawns a " pretty-name))
              :effects/target-all (fn [_ _ctx]
                                    "All visible targets")
              :entity/delete-after-duration (fn [[_ counter] {:keys [ctx/elapsed-time]}]
                                              (str "Remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
              :entity/faction (fn [[_ faction] _ctx]
                                (str "Faction: " (name faction)))
              :entity/fsm (fn [[_ fsm] _ctx]
                            (str "State: " (name (:state fsm))))
              :entity/modifiers (fn [[_ mods] _ctx]
                                  (when (seq mods)
                                    (str/join "\n" (keep (fn [[k ops]]
                                                           (op-info ops k)) mods))))
              :entity/skills (fn [[_ skills] _ctx]
                               ; => recursive info-text leads to endless text wall
                               (when (seq skills)
                                 (str "Skills: " (str/join "," (map name (keys skills))))))
              :entity/species (fn [[_ species] _ctx]
                                (str "Creature - " (str/capitalize (name species))))
              :entity/temp-modifier (fn [[_ {:keys [counter]}] {:keys [ctx/elapsed-time]}]
                                      (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio elapsed-time counter)) "/1"))
              :projectile/piercing? (fn [_ _ctx] ; TODO also when false ?!
                                      "Piercing")
              :property/pretty-name (fn [[_ v] _ctx]
                                      v)
              :skill/action-time (fn [[_ v] _ctx]
                                   (str "Action-Time: " (utils/readable-number v) " seconds"))
              :skill/action-time-modifier-key (fn [[_ v] _ctx]
                                                (case v
                                                  :entity/cast-speed "Spell"
                                                  :entity/attack-speed "Attack"))
              :skill/cooldown (fn [[_ v] _ctx]
                                (when-not (zero? v)
                                  (str "Cooldown: " (utils/readable-number v) " seconds")))
              :skill/cost (fn [[_ v] _ctx]
                            (when-not (zero? v)
                              (str "Cost: " v " Mana")))
              :maxrange (fn [[_ v] _ctx] v)}})

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(def ^:private mouseover-ellipse-width 5)

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn draw-item-on-cursor-state
  [{:keys [item]}
   entity
   {:keys [ctx/textures
           ctx/mouseover-actor
           ctx/world-mouse-position]}]
  (when (cdq.entity.state.player-item-on-cursor/world-item? mouseover-actor)
    [[:draw/texture-region
      (image/texture-region (:entity/image item) textures)
      (cdq.entity.state.player-item-on-cursor/item-place-position world-mouse-position entity)
      {:center? true}]]))

(defn draw-mouseover-highlighting [_ entity {:keys [ctx/player-eid]}]
  (let [player @player-eid
        faction (:entity/faction entity)]
    [[:draw/with-line-width mouseover-ellipse-width
      [[:draw/ellipse
        (entity/position entity)
        (/ (:body/width  (:entity/body entity)) 2)
        (/ (:body/height (:entity/body entity)) 2)
        (cond (= faction (faction/enemy (:entity/faction player)))
              enemy-color
              (= faction (:entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(defn draw-stunned-state [_ entity _ctx]
  [[:draw/circle
    (entity/position entity)
    stunned-circle-width
    stunned-circle-color]])

(defn draw-clickable-mouseover-text [{:keys [text]} {:keys [entity/mouseover?] :as entity} _ctx]
  (when (and mouseover? text)
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (/ (:body/height (:entity/body entity)) 2))
                    :up? true}]])))

(defn draw-skill-image [texture-region entity [x y] action-counter-ratio]
  (let [radius skill-image-radius-world-units
        y (+ (float y)
             (float (/ (:body/height (:entity/body entity)) 2))
             (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/texture-region texture-region [(- (float x) radius) y]]]))

(defn render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defn draw-active-skill [{:keys [skill effect-ctx counter]}
                         entity
                         {:keys [ctx/textures
                                 ctx/elapsed-time]
                          :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image (image/texture-region image textures)
                              entity
                              (entity/position entity)
                              (timer/ratio elapsed-time counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(defn draw-centered-rotated-image
  [image
   entity
   {:keys [ctx/textures]}]
  [[:draw/texture-region
    (image/texture-region image textures)
    (entity/position entity)
    {:center? true
     :rotation (or (:body/rotation-angle (:entity/body entity))
                   0)}]])

(defn call-render-image [animation entity ctx]
  (draw-centered-rotated-image (animation/current-frame animation)
                               entity
                               ctx))

(defn draw-line-entity [{:keys [thick? end color]} entity _ctx]
  (let [position (entity/position entity)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defn draw-sleeping-state [_ entity _ctx]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (/ (:body/height (:entity/body entity)) 2))
                  :up? true}]]))

; TODO draw opacity as of counter ratio?
(defn draw-temp-modifiers [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn draw-text-over-entity [{:keys [text]} entity {:keys [ctx/world-unit-scale]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 world-unit-scale))
                  :scale 2
                  :up? true}]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn draw-stats [_ entity {:keys [ctx/world-unit-scale]}]
  (let [ratio (val-max/ratio (stats/get-hitpoints (:creature/stats entity)))]
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar world-unit-scale
                  (:entity/body entity)
                  ratio))))

(def render-layers
  [{:entity/mouseover? draw-mouseover-highlighting
    :stunned draw-stunned-state
    :player-item-on-cursor draw-item-on-cursor-state}
   {:entity/clickable draw-clickable-mouseover-text
    :entity/animation call-render-image
    :entity/image draw-centered-rotated-image
    :entity/line-render draw-line-entity}
   {:npc-sleeping draw-sleeping-state
    :entity/temp-modifier draw-temp-modifiers
    :entity/string-effect draw-text-over-entity}
   {:creature/stats draw-stats
    :active-skill draw-active-skill}])

(extend-type Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))

(defn create [ctx]
  (cdq.game.effects/init!)
  (merge (map->Context
          {
           :schema (m/schema schema)
           :fsms {:fsms/player player-fsm
                  :fsms/npc npc-fsm}
           :entity-components entity-components
           :spawn-entity-schema components-schema
           :ui-actors '[[cdq.ui.dev-menu/create {:menus [{:label "World"
                                                          :items [cdq.ui.dev-menu.menus.select-world/create
                                                                  {:world-fns [[cdq.world-fns.tmx/create
                                                                                {:tmx-file "maps/vampire.tmx"
                                                                                 :start-position [32 71]}]
                                                                               [cdq.world-fns.uf-caves/create
                                                                                {:tile-size 48
                                                                                 :texture-path "maps/uf_terrain.png"
                                                                                 :spawn-rate 0.02
                                                                                 :scaling 3
                                                                                 :cave-size 200
                                                                                 :cave-style :wide}]
                                                                               [cdq.world-fns.modules/create
                                                                                {:world/map-size 5,
                                                                                 :world/max-area-level 3,
                                                                                 :world/spawn-rate 0.05}]]
                                                                   :reset-game-fn cdq.ui.dev-menu/reset-game-fn}
                                                                  ]}
                                                         {:label "Help"
                                                          :items [cdq.ui.dev-menu/help-items]}
                                                         {:label "Editor"
                                                          :items [cdq.ui.dev-menu.menus.db/create]}
                                                         {:label "Ctx Data"
                                                          :items [cdq.ui.dev-menu.menus.ctx-data-view/items]}]}]
                        [cdq.ui.action-bar/create {:id :action-bar}]
                        [cdq.ui.hp-mana-bar/create {:rahmen-file "images/rahmen.png"
                                                    :rahmenw 150
                                                    :rahmenh 26
                                                    :hpcontent-file "images/hp.png"
                                                    :manacontent-file "images/mana.png"
                                                    :y-mana 80}]
                        [cdq.ui.windows/create]
                        [cdq.ui.player-state-draw/create]
                        [cdq.ui.message/create {:duration-seconds 0.5
                                                :name "player-message"}]]
           :draw-on-world-viewport [cdq.draw-on-world-viewport.tile-grid/do!
                                    cdq.draw-on-world-viewport.cell-debug/do!
                                    cdq.draw-on-world-viewport.entities/do!
                                    ;cdq.draw-on-world-viewport.geom-test/do! : TODO can this be an independent test ?
                                    cdq.draw-on-world-viewport.highlight-mouseover-tile/do!]
           :config {:cdq.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                             :initial-state :npc-sleeping}
                                                :entity/faction :evil}
                    :cdq.game/player-props {:creature-id :creatures/vampire
                                            :components {:entity/fsm {:fsm :fsms/player
                                                                      :initial-state :player-idle}
                                                         :entity/faction :good
                                                         :entity/player? true
                                                         :entity/free-skill-points 3
                                                         :entity/clickable {:type :clickable/player}
                                                         :entity/click-distance-tiles 1.5}}
                    :cdq.reset-game-state/world {:content-grid-cell-size 16
                                                 :potential-field-factions-iterations {:good 15
                                                                                       :evil 5}}
                    :effect-body-props {:width 0.5
                                        :height 0.5
                                        :z-order :z-order/effect}

                    :controls {:zoom-in :minus
                               :zoom-out :equals
                               :unpause-once :p
                               :unpause-continously :space}}
           :draw-fns cdq.draw-impl/draw-fns
           :mouseover-eid nil
           :paused? nil
           :delta-time 2
           :active-entities 1
           :unit-scale (atom 1)
           :world-unit-scale (float (/ 48))
           :info info-configuration
           :db (cdq.db-impl/create {:schemas "schema.edn"
                                    :properties "properties.edn"})
           :render-layers render-layers
           })
         ctx))
