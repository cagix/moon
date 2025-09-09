(ns cdq.txs
  (:require [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.gdx.math.vector2 :as v]
            [cdq.image :as image]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.rand :refer [rand-int-between]]
            [cdq.skills :as skills]
            [cdq.stage :as stage]
            [cdq.stats :as stats]
            [cdq.timer :as timer]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [reduce-fsm :as fsm]))

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

   :tx/pickup-item (fn [[_ eid item] _ctx]
                     (inventory/assert-valid-item? item)
                     (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
                       (assert cell)
                       (assert (or (inventory/stackable? item cell-item)
                                   (nil? cell-item)))
                       (if (inventory/stackable? item cell-item)
                         (do
                          #_(tx/stack-item ctx eid cell item))
                         [[:tx/set-item eid cell item]])))

   :tx/set-cooldown (fn [[_ eid skill] {:keys [ctx/elapsed-time]}]
                      (swap! eid assoc-in
                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                             (timer/create elapsed-time (:skill/cooldown skill)))
                      nil)

   :tx/add-text-effect (fn [[_ eid text duration] {:keys [ctx/elapsed-time]}]
                         (swap! eid add-text-effect text duration elapsed-time)
                         nil)

   :tx/add-skill (fn [[_ eid skill] _ctx]
                   (swap! eid update :entity/skills skills/add-skill skill)
                   (if (:entity/player? @eid)
                     [[:tx/player-add-skill skill]]
                     nil))

   #_(defn remove-skill [eid {:keys [property/id] :as skill}]
       {:pre [(contains? (:entity/skills @eid) id)]}
       (swap! eid update :entity/skills dissoc id)
       (when (:entity/player? @eid)
         (remove-skill! ctx skill)))

   :tx/player-add-skill (fn [[_ skill] {:keys [ctx/textures
                                               ctx/stage]}]
                          (stage/add-skill! stage
                                            {:skill-id (:property/id skill)
                                             :texture-region (image/texture-region (:entity/image skill) textures)
                                             :tooltip-text (fn [ctx]
                                                             (info/generate (:ctx/info ctx) skill ctx))})
                          nil)

   #_(defn- remove-skill! [{:keys [ctx/stage]} skill]
       (stage/remove-skill! stage (:property/id skill))
       nil)

   :tx/set-item (fn [[_ eid cell item] _ctx]
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

   :tx/remove-item (fn [[_ eid cell] _ctx]
                     (let [entity @eid
                           item (get-in (:entity/inventory entity) cell)]
                       (assert item)
                       (swap! eid assoc-in (cons :entity/inventory cell) nil)
                       (when (inventory/applies-modifiers? cell)
                         (swap! eid update :creature/stats stats/remove (:entity/modifiers item)))
                       (if (:entity/player? entity)
                         [[:tx/player-remove-item cell]]
                         nil)))

   :tx/player-set-item (fn [[_ cell item]
                            {:keys [ctx/textures
                                    ctx/stage]
                             :as ctx}]
                         (stage/set-item! stage cell
                                          {:texture-region (image/texture-region (:entity/image item) textures)
                                           :tooltip-text (fn [ctx]
                                                           (info/generate (:ctx/info ctx) item ctx))})
                         nil)

   :tx/player-remove-item (fn [[_ cell] {:keys [ctx/stage]}]
                            (stage/remove-item! stage cell)
                            nil)

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

   :tx/toggle-inventory-visible (fn [_ {:keys [ctx/stage]}]
                                  (stage/toggle-inventory-visible! stage)
                                  nil)
   :tx/show-message (fn
                      [[_ message]
                       {:keys [ctx/stage]}]
                      (stage/show-text-message! stage message)
                      nil)

   :tx/show-modal (fn [[_ opts] {:keys [ctx/stage
                                        ctx/ui-viewport]}]
                    (stage/show-modal-window! stage ui-viewport opts)
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

   :tx/move-entity (fn [params ctx]
                     (world/move-entity! ctx params))

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

   :tx/spawn-entity (fn [[_ components] ctx]
                      (world/spawn-entity! ctx components))})
