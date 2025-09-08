(ns cdq.application
  (:require [cdq.application.colors :as colors]
            [cdq.application.config :as config]
            [cdq.application.context-record :as context-record]
            [cdq.application.draw-on-world-viewport :as draw-on-world-viewport]
            [cdq.application.draw-impl :as draw-impl]
            [cdq.application.effects :as effects]
            [cdq.application.entity-components :as entity-components]
            [cdq.application.entity-states]
            [cdq.application.fsms :as fsms]
            [cdq.application.gdx-create]
            [cdq.application.info :as application.info]
            [cdq.application.lwjgl :as lwjgl]
            [cdq.application.reset-game-state]
            [cdq.application.render-layers :as render-layers]
            [cdq.application.tx-spawn-schema :as tx-spawn-schema]
            [cdq.application.os-settings :as os-settings]
            [cdq.application.ui-actors :as ui-actors]
            [cdq.db-impl]
            [cdq.files]
            [cdq.animation :as animation]
            [cdq.audio]
            [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.body :as body]
            cdq.entity-tick
            [cdq.entity.state :as state]
            [cdq.gdx.math.vector2 :as v]
            [cdq.grid :as grid]
            [cdq.grid.cell :as cell]
            [cdq.image :as image]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.malli :as m]
            [cdq.application.effects]
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
            [cdq.utils.tiled :as tiled]
            [cdq.world :as world]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [clojure.gdx.scenes.scene2d.ui.button :as button]
            [clojure.vis-ui.widget :as widget]
            [clojure.vis-ui.window :as window]
            [reduce-fsm :as fsm])
  (:gen-class))

(def starting-world
  '[cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                              :start-position [32 71]}])

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
  (let [f (get txs-fn-map k)]
    (assert f (pr-str k))
    (f component ctx)))

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
               (cdq.audio/play-sound! audio sound-name)
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

(extend-type cdq.application.context_record.Context
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

(extend-type cdq.application.context_record.Context
  clojure.gdx.scenes.scene2d/Context
  (handle-draws! [ctx draws]
    (cdq.ctx/handle-draws! ctx draws)))

(def state (atom nil))

(defn -main []
  (reduce (fn [ctx f]
            (if (vector? f)
              (let [[f params] f]
                (f ctx params))
              (f ctx)))
          (config/load "ctx.edn")
          [context-record/create
           effects/init!
           #(assoc %
                   :ctx/application-state state
                   :ctx/fsms fsms/k->fsm
                   :ctx/entity-components entity-components/method-mappings
                   :ctx/spawn-entity-schema tx-spawn-schema/components-schema
                   :ctx/ui-actors ui-actors/create-stuff
                   :ctx/draw-on-world-viewport draw-on-world-viewport/draw-fns
                   :ctx/config {:cdq.game/enemy-components {:entity/fsm {:fsm :fsms/npc
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
                                :world {:content-grid-cell-size 16
                                        :potential-field-factions-iterations {:good 15
                                                                              :evil 5}}
                                :effect-body-props {:width 0.5
                                                    :height 0.5
                                                    :z-order :z-order/effect}

                                :controls {:zoom-in :minus
                                           :zoom-out :equals
                                           :unpause-once :p
                                           :unpause-continously :space}}
                   :ctx/draw-fns draw-impl/draw-fns
                   :ctx/mouseover-eid nil
                   :ctx/paused? nil
                   :ctx/delta-time 2
                   :ctx/active-entities 1
                   :ctx/unit-scale (atom 1)
                   :ctx/world-unit-scale (float (/ 48))
                   :ctx/info application.info/info-configuration
                   :ctx/db (cdq.db-impl/create {:schemas "schema.edn"
                                                :properties "properties.edn"})
                   :ctx/render-layers render-layers/render-layers)
           os-settings/handle!
           colors/define-gdx-colors!
           [lwjgl/start-gdx-app (fn [ctx]
                                  (cdq.application.reset-game-state/reset-game-state!
                                   (cdq.application.gdx-create/after-gdx-create! ctx)
                                   starting-world))]]))

(.bindRoot #'cdq.entity-tick/entity->tick entity->tick)
