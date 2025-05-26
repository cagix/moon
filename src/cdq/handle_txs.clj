(ns cdq.handle-txs
  (:require [cdq.animation :as animation]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.projectile :as projectile]
            [cdq.ui.message]
            [cdq.vector2 :as v]
            [gdl.application]
            [gdl.ui :as ui]
            [gdl.utils :as utils]
            [reduce-fsm :as fsm]))

; TODO move-entity!

(def sound-path-format "sounds/%s.wav")

(defn- play-sound! [ctx sound-name]
  (->> sound-name
       (format sound-path-format)
       (g/sound ctx)
       com.badlogic.gdx.audio.Sound/.play))

(defn- spawn-effect! [ctx position components]
  (g/spawn-entity! ctx
                   position
                   (g/config ctx :effect-body-props)
                   components))

(defmulti handle-tx! (fn [[k & _params] _ctx]
                       k))

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

(defmethod handle-tx! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value))

(defmethod handle-tx! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value))

(defmethod handle-tx! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k))

; => this means we dont want gdl context in our app open --- hide it again o.lo
(defmethod handle-tx! :tx/sound [[_ sound-name] ctx]
  (play-sound! ctx sound-name))

(defmethod handle-tx! :tx/set-cursor [[_ cursor-key] ctx]
  (g/set-cursor! ctx (utils/safe-get (:ctx/cursors ctx) cursor-key)))

(defmethod handle-tx! :tx/show-message [[_ message] ctx]
  (cdq.ui.message/show! (g/find-actor-by-name ctx "player-message")
                        message))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defmethod handle-tx! :tx/show-modal [[_ {:keys [title text button-text on-click]}] ctx]
  (assert (not (g/get-actor ctx ::modal)))
  (g/add-actor! ctx
                (ui/window {:title title
                            :rows [[(ui/label text)]
                                   [(ui/text-button button-text
                                                    (fn [_actor ctx]
                                                      (ui/remove! (g/get-actor ctx ::modal))
                                                      (on-click)))]]
                            :id ::modal
                            :modal? true
                            :center-position [(/ (g/ui-viewport-width ctx) 2)
                                              (* (g/ui-viewport-height ctx) (/ 3 4))]
                            :pack? true})))

(defmethod handle-tx! :tx/toggle-inventory-visible [_ ctx]
  (-> (g/get-actor ctx :windows)
      :inventory-window
      ui/toggle-visible!))

(defmethod handle-tx! :tx/audiovisual [[_ position {:keys [tx/sound entity/animation]}] ctx]
  (play-sound! ctx sound)
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
  (g/spawn-creature! ctx opts))

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

(defmethod handle-tx! :tx/move-entity [[_ eid body direction rotate-in-movement-direction?] ctx]
  (g/context-entity-moved! ctx eid)
  (swap! eid assoc
         :position (:position body)
         :left-bottom (:left-bottom body))
  (when rotate-in-movement-direction?
    (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))

; we cannot just set/unset movement direction
; because it is handled by the state enter/exit for npg/player movement state ...
; so we cannot expose it as a 'transaction'
; so the movement should be updated in the respective npg/player movement 'state' and no movement 'component' necessary !
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
