(ns cdq.game.create.txs
  (:require [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.graphics :as graphics]
            [cdq.ui :as ui]
            [cdq.ui.stage :as stage]
            [cdq.world :as world]
            [cdq.world.info :as info]
            [cdq.world.tx.spawn-entity]
            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]
            [clojure.utils :as utils]))

(require '[cdq.entity.skills :as skills])
(require '[clojure.timer :as timer])
(require '[cdq.entity.inventory :as inventory]
         '[cdq.entity.stats :as stats])
(require '[cdq.entity.state :as state]
         '[reduce-fsm :as fsm]
         '[cdq.effect :as effect])
(require '[cdq.world.content-grid :as content-grid]
         '[cdq.world.grid :as grid]
         '[clojure.math.vector2 :as v])

(def reaction-txs-fn-map
  {
   :tx/sound                    (fn [{:keys [ctx/audio]} sound-name]
                                  (audio/play! audio sound-name)
                                  nil)

   :tx/set-item    (fn
                     [{:keys [ctx/graphics
                              ctx/stage]}
                      eid cell item]
                     (when (:entity/player? @eid)
                       (ui/set-item! stage cell
                                     {:texture-region (graphics/texture-region graphics (:entity/image item))
                                      :tooltip-text (info/text item nil)})
                       nil))

   :tx/remove-item (fn
                     [{:keys [ctx/stage]}
                      eid cell]
                     (when (:entity/player? @eid)
                       (ui/remove-item! stage cell)
                       nil))

   :tx/add-skill   (fn
                     [{:keys [ctx/graphics
                              ctx/stage]}
                      eid skill]
                     (when (:entity/player? @eid)
                       (ui/add-skill! stage
                                      {:skill-id (:property/id skill)
                                       :texture-region (graphics/texture-region graphics (:entity/image skill))
                                       :tooltip-text (fn [{:keys [ctx/world]}]
                                                       (info/text skill world))})
                       nil))
   }
  )

(defn world-move-entity
  [{:keys [world/content-grid
           world/grid]}
   eid body direction rotate-in-movement-direction?]
  (content-grid/position-changed! content-grid eid)
  (grid/remove-from-touched-cells! grid eid)
  (grid/set-touched-cells! grid eid)
  (when (:body/collides? (:entity/body @eid))
    (grid/remove-from-occupied-cells! grid eid)
    (grid/set-occupied-cells! grid eid))
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)

(defn world-handle-event
  ([world eid event]
   (world-handle-event world eid event nil))
  ([world eid event params]
   (let [fsm (:entity/fsm @eid)
         _ (assert fsm)
         old-state-k (:state fsm)
         new-fsm (fsm/fsm-event fsm event)
         new-state-k (:state new-fsm)]
     (when-not (= old-state-k new-state-k)
       (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                             [k (k @eid)])
             new-state-obj [new-state-k (state/create [new-state-k params] eid world)]]
         [[:tx/assoc       eid :entity/fsm new-fsm]
          [:tx/assoc       eid new-state-k (new-state-obj 1)]
          [:tx/dissoc      eid old-state-k]
          [:tx/state-exit  eid old-state-obj]
          [:tx/state-enter eid new-state-obj]])))))

(def txs-fn-map
  {
   :tx/assoc                    (fn [_ctx eid k value]
                                  (swap! eid assoc k value)
                                  nil)

   :tx/assoc-in                 (fn [_ctx eid ks value]
                                  (swap! eid assoc-in ks value)
                                  nil)

   :tx/dissoc                   (fn [_ctx eid k]
                                  (swap! eid dissoc k)
                                  nil)

   :tx/update                   (fn [_ctx eid & params]
                                  (apply swap! eid update params)
                                  nil)

   :tx/mark-destroyed           (fn [_ctx eid]
                                  (swap! eid assoc :entity/destroyed? true)
                                  nil)

   :tx/set-cooldown             (fn [{:keys [ctx/world]} eid skill]
                                  (swap! eid update :entity/skills skills/set-cooldown skill (:world/elapsed-time world))
                                  nil)

   :tx/add-text-effect          (fn [{:keys [ctx/world]} eid text duration]
                                  [[:tx/assoc
                                    eid
                                    :entity/string-effect
                                    (if-let [existing (:entity/string-effect @eid)]
                                      (-> existing
                                          (update :text str "\n" text)
                                          (update :counter timer/increment duration))
                                      {:text text
                                       :counter (timer/create (:world/elapsed-time world) duration)})]])

   :tx/add-skill                (fn [_ctx eid {:keys [property/id] :as skill}]
                                  {:pre [(not (contains? (:entity/skills @eid) id))]}
                                  (swap! eid update :entity/skills assoc id skill)
                                  nil)

   :tx/set-item                 (fn [_ctx eid cell item]
                                  (let [entity @eid
                                        inventory (:entity/inventory entity)]
                                    (assert (and (nil? (get-in inventory cell))
                                                 (inventory/valid-slot? cell item)))
                                    (swap! eid assoc-in (cons :entity/inventory cell) item)
                                    (when (inventory/applies-modifiers? cell)
                                      (swap! eid update :entity/stats stats/add (:stats/modifiers item)))
                                    nil))

   :tx/remove-item              (fn [_ctx eid cell]
                                  (let [entity @eid
                                        item (get-in (:entity/inventory entity) cell)]
                                    (assert item)
                                    (swap! eid assoc-in (cons :entity/inventory cell) nil)
                                    (when (inventory/applies-modifiers? cell)
                                      (swap! eid update :entity/stats stats/remove-mods (:stats/modifiers item)))
                                    nil))

   :tx/pickup-item              (fn [_ctx eid item]
                                  (inventory/assert-valid-item? item)
                                  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
                                    (assert cell)
                                    (assert (or (inventory/stackable? item cell-item)
                                                (nil? cell-item)))
                                    (if (inventory/stackable? item cell-item)
                                      (do
                                       #_(tx/stack-item ctx eid cell item))
                                      [[:tx/set-item eid cell item]])))

   :tx/event                    (fn [{:keys [ctx/world]} & params]
                                  (apply world-handle-event world params))

   :tx/state-exit               (fn [ctx eid [state-k state-v]]
                                  (state/exit [state-k state-v] eid ctx))

   :tx/state-enter              (fn [_ctx eid [state-k state-v]]
                                  (state/enter [state-k state-v] eid))

   :tx/effect                   (fn [{:keys [ctx/world]} effect-ctx effects]
                                  (mapcat #(effect/handle % effect-ctx world)
                                          (filter #(effect/applicable? % effect-ctx) effects)))

   :tx/audiovisual              (fn
                                  [{:keys [ctx/db]} position audiovisual]
                                  (let [{:keys [tx/sound
                                                entity/animation]} (if (keyword? audiovisual)
                                                                     (db/build db audiovisual)
                                                                     audiovisual)]
                                    [[:tx/sound sound]
                                     [:tx/spawn-effect
                                      position
                                      {:entity/animation (assoc animation :delete-after-stopped? true)}]]))

   :tx/spawn-alert              (fn [{:keys [ctx/world]} position faction duration]
                                  [[:tx/spawn-effect
                                    position
                                    {:entity/alert-friendlies-after-duration
                                     {:counter (timer/create (:world/elapsed-time world) duration)
                                      :faction faction}}]])

   :tx/spawn-line               (fn [_ctx {:keys [start end duration color thick?]}]
                                  [[:tx/spawn-effect
                                    start
                                    {:entity/line-render {:thick? thick? :end end :color color}
                                     :entity/delete-after-duration duration}]])

   :tx/move-entity              (fn [{:keys [ctx/world]} & params]
                                  (apply world-move-entity world params))

   :tx/spawn-projectile         (fn
                                  [_ctx
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

   :tx/spawn-effect             (fn
                                  [{:keys [ctx/world]}
                                   position
                                   components]
                                  [[:tx/spawn-entity
                                    (assoc components
                                           :entity/body (assoc (:world/effect-body-props world) :position position))]])

   :tx/spawn-item               (fn [_ctx position item]
                                  [[:tx/spawn-entity
                                    {:entity/body {:position position
                                                   :width 0.75
                                                   :height 0.75
                                                   :z-order :z-order/on-ground}
                                     :entity/image (:entity/image item)
                                     :entity/item item
                                     :entity/clickable {:type :clickable/item
                                                        :text (:property/pretty-name item)}}]])

   :tx/spawn-creature           (fn
                                  [_ctx
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

   :tx/spawn-entity             (fn [{:keys [ctx/world]} entity]
                                  (cdq.world.tx.spawn-entity/do! world entity))

   :tx/sound                    (fn [_ctx sound-name]
                                  nil)

   :tx/toggle-inventory-visible (fn [{:keys [ctx/stage]}]
                                  (ui/toggle-inventory-visible! stage)
                                  nil)
   :tx/show-message             (fn [{:keys [ctx/stage]} message]
                                  (ui/show-text-message! stage message)
                                  nil)
   :tx/show-modal               (fn [{:keys [ctx/stage]} opts]
                                  (ui/show-modal-window! stage (stage/viewport stage) opts)
                                  nil)
   }
  )

(defn do! [ctx]
  (extend-type (class ctx)
    txs/TransactionHandler
    (handle! [ctx txs]
      (let [handled-txs (tx-handler/actions! txs-fn-map ctx txs)]
        (tx-handler/actions! reaction-txs-fn-map
                             ctx
                             handled-txs
                             :strict? false))))
  ctx)
