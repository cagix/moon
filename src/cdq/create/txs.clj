(ns cdq.create.txs
  (:require [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.editor :as editor]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.inventory :as inventory]
            [cdq.stage]
            [cdq.stats :as stats]
            [cdq.string :as string]
            [cdq.timer :as timer]
            cdq.tx.open-property-editor
            cdq.tx.spawn-creature
            cdq.tx.spawn-entity
            cdq.tx.update-potential-fields
            [cdq.ui.editor.map-widget-table :as map-widget-table]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [clj-commons.pretty.repl :as pretty-repl]
            [clojure.math.vector2 :as v]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as group]
            [clojure.scene2d.stage :as stage]
            [clojure.rand :refer [rand-int-between]]
            [clojure.repl]
            [reduce-fsm :as fsm]))

(defn- handle-event
  ([world eid event]
   (handle-event world eid event nil))
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

(defn calc-damage
  ([source target damage]
   (update (calc-damage source damage)
           :damage/min-max
           cdq.entity.stats/apply-max
           (:entity/modifiers target)
           :modifier/damage-receive-max))
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (cdq.entity.stats/apply-min (:entity/modifiers source) :modifier/damage-deal-min)
                (cdq.entity.stats/apply-max (:entity/modifiers source) :modifier/damage-deal-max)))))

; not in stats because projectile as source doesnt have stats
; FIXME I don't see it triggering with 10 armor save ... !
(defn- effective-armor-save [source-stats target-stats]
  (max (- (or (cdq.stats/get-stat-value source-stats :entity/armor-save)   0)
          (or (cdq.stats/get-stat-value target-stats :entity/armor-pierce) 0))
       0))

(comment

 (effective-armor-save {} {:entity/modifiers {:modifiers/armor-save {:op/inc 10}}
                           :entity/armor-save 0})
 ; broken
 (let [source* {:entity/armor-pierce 0.4}
       target* {:entity/armor-save   0.5}]
   (effective-armor-save source* target*))
 )

(def print-level 3)
(def print-depth 24)

(def txs-fn-map
  {
   :tx/rebuild-editor-window (fn
                               [{:keys [ctx/db
                                        ctx/stage]}]
                               (let [window (-> stage
                                                stage/root
                                                (group/find-actor "cdq.ui.editor.window"))
                                     map-widget-table (-> window
                                                          (group/find-actor "cdq.ui.widget.scroll-pane-table")
                                                          (group/find-actor "scroll-pane-table")
                                                          (group/find-actor "cdq.schema.map.ui.widget"))
                                     property (map-widget-table/get-value map-widget-table (:db/schemas db))]
                                 (actor/remove! window)
                                 [[:tx/open-property-editor property]]))
   :tx/assoc (fn [_ctx eid k value]
               (swap! eid assoc k value)
               nil)
   :tx/assoc-in (fn [_ctx eid ks value]
                  (swap! eid assoc-in ks value)
                  nil)
   :tx/dissoc (fn [_ctx eid k]
                (swap! eid dissoc k)
                nil)
   :tx/effect (fn [ctx effect-ctx effects]
                (mapcat #(effect/handle % effect-ctx ctx)
                        (effect/filter-applicable? effect-ctx effects)))
   :tx/mark-destroyed (fn [_ctx eid]
                        (swap! eid assoc :entity/destroyed? true)
                        nil)
   :tx/mod-add (fn [_ctx eid modifiers]
                 (swap! eid update :creature/stats stats/add modifiers)
                 nil)
   :tx/mod-remove (fn do! [_ctx eid modifiers]
                    (swap! eid update :creature/stats stats/remove-mods modifiers)
                    nil)
   :tx/pay-mana-cost (fn do! [_ctx eid cost]
                       (swap! eid update :creature/stats stats/pay-mana-cost cost)
                       nil)
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
   :tx/print-stacktrace (fn [_ctx throwable]
                          (binding [*print-level* print-level]
                            (pretty-repl/pretty-pst throwable print-depth))
                          nil)
   :tx/show-error-window (fn [{:keys [ctx/stage]} throwable]
                           (stage/add! stage (scene2d/build
                                              {:actor/type :actor.type/window
                                               :title "Error"
                                               :rows [[{:actor {:actor/type :actor.type/label
                                                                :label/text (binding [*print-level* 3]
                                                                              (string/with-err-str
                                                                                (clojure.repl/pst throwable)))}}]]
                                               :modal? true
                                               :close-button? true
                                               :close-on-escape? true
                                               :center? true
                                               :pack? true})))
   :tx/set-cooldown (fn
                      [{:keys [ctx/world]}
                       eid skill]
                      (swap! eid assoc-in
                             [:entity/skills (:property/id skill) :skill/cooling-down?]
                             (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
                      nil)
   :tx/add-text-effect (fn
                         [{:keys [ctx/world]}
                          eid
                          text
                          duration]
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

   :tx/player-add-skill (fn
                          [{:keys [ctx/graphics
                                   ctx/stage]}
                           skill]
                          (cdq.stage/add-skill! stage
                                            {:skill-id (:property/id skill)
                                             :texture-region (graphics/texture-region graphics (:entity/image skill))
                                             :tooltip-text (fn [{:keys [ctx/world]}]
                                                             (world/info-text world skill))})
                          nil)
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
   :tx/player-set-item (fn
                         [{:keys [ctx/graphics
                                  ctx/stage]}
                          cell item]
                         (cdq.stage/set-item! stage cell
                                          {:texture-region (graphics/texture-region graphics (:entity/image item))
                                           :tooltip-text (fn [{:keys [ctx/world]}]
                                                           (world/info-text world item))})
                         nil)
   :tx/player-remove-item (fn
                            [{:keys [ctx/stage]}
                             cell]
                            (cdq.stage/remove-item! stage cell)
                            nil)
   :tx/event (fn [{:keys [ctx/world]} & params]
               (apply handle-event world params))
   :tx/toggle-inventory-visible (fn [{:keys [ctx/stage]}]
                                  (cdq.stage/toggle-inventory-visible! stage)
                                  nil)
   :tx/show-message (fn
                      [{:keys [ctx/stage]}
                       message]
                      (cdq.stage/show-text-message! stage message)
                      nil)
   :tx/show-modal (fn
                    [{:keys [ctx/stage]}
                     opts]
                    (cdq.stage/show-modal-window! stage (clojure.scene2d.stage/viewport stage) opts)
                    nil)
   :tx/sound (fn
               [{:keys [ctx/audio]}
                sound-name]
               (audio/play-sound! audio sound-name)
               nil)
   :tx/state-exit (fn
                    [ctx eid [state-k state-v]]
                    (state/exit [state-k state-v] eid ctx))
   :tx/state-enter (fn
                     [_ctx eid [state-k state-v]]
                     (state/enter [state-k state-v] eid))
   :tx/audiovisual (fn
                     [{:keys [ctx/db]}
                      position
                      audiovisual]
                     (let [{:keys [tx/sound
                                   entity/animation]} (if (keyword? audiovisual)
                                                        (db/build db audiovisual)
                                                        audiovisual)]
                       [[:tx/sound sound]
                        [:tx/spawn-effect
                         position
                         {:entity/animation animation
                          :entity/delete-after-animation-stopped? true}]]))
   :tx/spawn-alert (fn
                     [{:keys [ctx/world]}
                      position faction duration]
                     [[:tx/spawn-effect
                       position
                       {:entity/alert-friendlies-after-duration
                        {:counter (timer/create (:world/elapsed-time world) duration)
                         :faction faction}}]])
   :tx/spawn-line (fn
                    [_ctx {:keys [start end duration color thick?]}]
                    [[:tx/spawn-effect
                      start
                      {:entity/line-render {:thick? thick? :end end :color color}
                       :entity/delete-after-duration duration}]])
   :tx/deal-damage (fn
                     [_ctx
                      source
                      target
                      damage]
                     (let [source* @source
                           target* @target
                           hp (cdq.stats/get-hitpoints (:creature/stats target*))]
                       (cond
                        (zero? (hp 0))
                        nil

                        ; TODO find a better way
                        (not (:creature/stats target*))
                        nil

                        (and (:creature/stats source*)
                             (:creature/stats target*)
                             (< (rand) (effective-armor-save (:creature/stats source*)
                                                             (:creature/stats target*))))
                        [[:tx/add-text-effect target "[WHITE]ARMOR" 0.3]]

                        :else
                        (let [min-max (:damage/min-max (calc-damage (:creature/stats source*)
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
                     [{:keys [ctx/world]} eid body direction rotate-in-movement-direction?]
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
   :tx/open-editor-overview (fn
                              [{:keys [ctx/db
                                       ctx/graphics
                                       ctx/stage]}
                               {:keys [property-type
                                       clicked-id-fn]}]
                              (stage/add! stage (scene2d/build
                                                 {:actor/type :actor.type/window
                                                  :title "Edit"
                                                  :modal? true
                                                  :close-button? true
                                                  :center? true
                                                  :close-on-escape? true
                                                  :pack? true
                                                  :rows (editor/overview-table-rows db
                                                                                    graphics
                                                                                    property-type
                                                                                    clicked-id-fn)})))
   :tx/open-property-editor cdq.tx.open-property-editor/do!
   :tx/spawn-projectile (fn
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
:tx/spawn-effect (fn
                   [{:keys [ctx/world]}
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
   :tx/spawn-creature cdq.tx.spawn-creature/do!
   :tx/spawn-entity cdq.tx.spawn-entity/do!
   :tx/update-potential-fields cdq.tx.update-potential-fields/do!
   })
