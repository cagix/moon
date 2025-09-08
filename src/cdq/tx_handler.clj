; audio
; world
; ui /tx\/
(ns cdq.tx-handler
  (:require [cdq.audio :as audio]

            [cdq.ctx :as ctx] ; ???

            [cdq.content-grid :as content-grid]

            [cdq.db :as db] ; ??

            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.entity.state.player-item-on-cursor]
            [cdq.gdx.math.vector2 :as v]
            [cdq.grid :as grid]
            [cdq.image :as image]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.malli :as m]
            [cdq.rand :refer [rand-int-between]]

            ; ??
            [cdq.render.handle-key-input :refer [toggle-inventory-visible!]]

            [cdq.stats :as stats]
            [cdq.timer :as timer]

            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.message]
            [cdq.ui.windows.inventory :as inventory-window]

            [cdq.utils :as utils]
            [cdq.world :as world]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.vis-ui.widget :as widget]
            [reduce-fsm :as fsm]))

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

(declare fn-map)

(defn do!
  [{k 0 :as component}
   ctx]
  ((get fn-map k) component ctx))

(def fn-map
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
                                      toggle-inventory-visible!)
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
                    (when-let [f (state-k state->exit)]
                      (f state-v eid ctx)))

   :tx/state-enter (fn [[_ eid [state-k state-v]] _ctx]
                     (when-let [f (state-k state->enter)]
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

(defn extend-ctx [ctx]
  (extend-type (class ctx)
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
          handled)))))
