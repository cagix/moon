(ns cdq.application
  (:require cdq.impl.db
            cdq.impl.ui
            cdq.impl.world
            cdq.info-impl
            [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.files :as files]
            [cdq.graphics :as graphics]
            [cdq.graphics.draws :as draws]
            [cdq.graphics.camera :as camera]
            [cdq.graphics.textures :as textures]
            [cdq.graphics.tiled-map-renderer :as tiled-map-renderer]
            [cdq.graphics.impl]
            [cdq.graphics.ui-viewport :as ui-viewport]
            [cdq.graphics.world-viewport :as world-viewport]
            [clojure.gdx.graphics.color :as color]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [clojure.scene2d :as scene2d]
            [cdq.effect :as effect]
            cdq.entity.animation
            [cdq.entity.body :as body]
            cdq.entity.delete-after-duration
            cdq.entity.projectile-collision
            cdq.entity.fsm
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.entity.stats :as stats]
            [cdq.entity.skills :as skills]
            [cdq.entity.skills.skill :as skill]
            [clojure.timer :as timer]
            [cdq.world :as world]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster :as raycaster]
            [cdq.world-fns.creature-tiles]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.math.geom :as geom]
            [clojure.math.vector2 :as v]
            [clojure.tx-handler :as tx-handler]
            [clojure.txs :as txs]
            [clojure.throwable :as throwable]
            [clojure.info :as info]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.utils :as utils]
            [malli.core :as m]
            [malli.utils :as mu]
            [qrecord.core :as q]
            [reduce-fsm :as fsm])
  (:import (cdq.ui Stage)
           (com.badlogic.gdx ApplicationListener
                             Files
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (org.lwjgl.system Configuration))
  (:gen-class))


(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(textures/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(def ^:private world-params
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
   :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                         :initial-state :npc-sleeping}
                            :entity/faction :evil}
   :world/player-components {:creature-id :creatures/vampire
                             :components {:entity/fsm {:fsm :fsms/player
                                                       :initial-state :player-idle}
                                          :entity/faction :good
                                          :entity/player? true
                                          :entity/free-skill-points 3
                                          :entity/clickable {:type :clickable/player}
                                          :entity/click-distance-tiles 1.5}}
   :world/effect-body-props {:width 0.5
                             :height 0.5
                             :z-order :z-order/effect}})

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle! ctx
               [[:tx/spawn-creature (let [{:keys [creature-id
                                                  components]} (:world/player-components world)]
                                      {:position (mapv (partial + 0.5) (:world/start-position world))
                                       :creature-property (db/build db creature-id)
                                       :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle!
   ctx
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn create-world
  [{:keys [ctx/db
           ctx/graphics
           ctx/world]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (-> ctx
        (assoc :ctx/world (cdq.impl.world/create world-params world-fn-result))
        spawn-player!
        spawn-enemies!)))

(def ^:private render-layers
  (map
   #(update-vals % requiring-resolve)
   '[{:entity/mouseover?     cdq.entity.mouseover.draw/txs
      :stunned               cdq.entity.state.stunned.draw/txs
      :player-item-on-cursor cdq.entity.state.player-item-on-cursor.draw/txs}
     {:entity/clickable      cdq.entity.clickable.draw/txs
      :entity/animation      cdq.entity.animation.draw/txs
      :entity/image          cdq.entity.image.draw/txs
      :entity/line-render    cdq.entity.line-render.draw/txs}
     {:npc-sleeping          cdq.entity.state.npc-sleeping.draw/txs
      :entity/temp-modifier  cdq.entity.temp-modifier.draw/txs
      :entity/string-effect  cdq.entity.string-effect.draw/txs}
     {:entity/stats          cdq.entity.stats.draw/txs
      :active-skill          cdq.entity.state.active-skill.draw/txs}]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (draws/handle! graphics (draw-body-rect (:entity/body entity)
                                                  (if (:body/collides? (:entity/body entity))
                                                    color/white
                                                    color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (draws/handle! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (draws/handle! graphics (draw-body-rect (:entity/body entity) color/red))
         (throwable/pretty-pst t))))

(defn draw-entities
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(def ^:private create-fns
  {:entity/animation             cdq.entity.animation/create
   :entity/body                  cdq.entity.body/create
   :entity/delete-after-duration cdq.entity.delete-after-duration/create
   :entity/projectile-collision  cdq.entity.projectile-collision/create
   :entity/stats                 cdq.entity.stats/create})

(defn- create-component [[k v] world]
  (if-let [f (create-fns k)]
    (f v world)
    v))

(def ^:private create!-fns
  {:entity/fsm                             cdq.entity.fsm/create!
   :entity/inventory                       cdq.entity.inventory/create!
   :entity/skills                          cdq.entity.skills/create!})

(defn- after-create-component [[k v] eid world]
  (when-let [f (create!-fns k)]
    (f v eid world)))

(q/defrecord Entity [entity/body])

(defn render-stage
  [{:keys [^Stage ctx/stage]
    :as ctx}]
  (set! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
  (.ctx  stage))

(def zoom-speed 0.025)

(defn window-camera-controls
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (camera/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (camera/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (ui/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (ui/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (ui/toggle-entity-info-window! stage))
  ctx)

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(defn remove-destroyed-entities
  [{:keys [ctx/world]
    :as ctx}]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid]} world]
    (doseq [eid (filter (comp :entity/destroyed? deref)
                        (vals @entity-ids))]
      (let [id (:entity/id @eid)]
        (assert (contains? @entity-ids id))
        (swap! entity-ids dissoc id))
      (content-grid/remove-entity! content-grid eid)
      (grid/remove-from-touched-cells! grid eid)
      (when (:body/collides? (:entity/body @eid))
        (grid/remove-from-occupied-cells! grid eid))
      (txs/handle! ctx
                   (mapcat (fn [[k v]]
                             (when-let [destroy! (:destroy! (k destroy-components))]
                               (destroy! v eid ctx)))
                           @eid))))
  ctx)

(defn tick-entities
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (txs/handle! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (:body/position (:entity/body @mouseover-eid)))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:body/position (:entity/body @player-eid))
                                           target-position)}))

(defn- interaction-state
  [stage
   world-mouse-position
   mouseover-eid
   player-eid
   mouseover-actor]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor (ui/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (body/distance (:entity/body @player-eid)
                                        (:entity/body @mouseover-eid))
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (ui/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state skill entity effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn assoc-interaction-state
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (assoc ctx :ctx/interaction-state (interaction-state stage
                                                       (:graphics/world-mouse-position graphics)
                                                       (:world/mouseover-eid world)
                                                       (:world/player-eid    world)
                                                       (ui/mouseover-actor stage (input/mouse-position input)))))

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/actor-fns :some]
    [:ctx/world :some]]))

(defn- validate [ctx]
  (mu/validate-humanize schema ctx)
  ctx)

(q/defrecord Context [])

(defn- player-add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (ui/add-skill! stage
                 {:skill-id (:property/id skill)
                  :texture-region (textures/texture-region graphics (:entity/image skill))
                  :tooltip-text (fn [{:keys [ctx/world]}]
                                  (info/text skill world))})
  nil)

(defn- player-set-item!
  [{:keys [ctx/graphics
           ctx/stage]}
   cell item]
  (ui/set-item! stage cell
                {:texture-region (textures/texture-region graphics (:entity/image item))
                 :tooltip-text (info/text item nil)})
  nil)

(defn player-remove-item! [{:keys [ctx/stage]}
                           cell]
  (ui/remove-item! stage cell)
  nil)

(defn toggle-inventory-visible! [{:keys [ctx/stage]}]
  (ui/toggle-inventory-visible! stage)
  nil)

(defn show-message! [{:keys [ctx/stage]} message]
  (ui/show-text-message! stage message)
  nil)

(defn show-modal! [{:keys [^Stage ctx/stage]} opts]
  (ui/show-modal-window! stage (.getViewport stage) opts)
  nil)

(defn handle-event
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

(def ^:private txs-fn-map
  {
   :tx/assoc (fn [_ctx eid k value]
               (swap! eid assoc k value)
               nil)
   :tx/assoc-in (fn [_ctx eid ks value]
                  (swap! eid assoc-in ks value)
                  nil)
   :tx/dissoc (fn [_ctx eid k]
                (swap! eid dissoc k)
                nil)
   :tx/update (fn [_ctx eid & params]
                (apply swap! eid update params)
                nil)
   :tx/mark-destroyed (fn [_ctx eid]
                        (swap! eid assoc :entity/destroyed? true)
                        nil)
   :tx/set-cooldown (fn [{:keys [ctx/world]} eid skill]
                      (swap! eid update :entity/skills skills/set-cooldown skill (:world/elapsed-time world))
                      nil)
   :tx/add-text-effect (fn [{:keys [ctx/world]} eid text duration]
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
                   nil)

   #_(defn remove-skill [_ctx eid {:keys [property/id] :as skill}]
       {:pre [(contains? (:entity/skills @eid) id)]}
       (swap! eid update :entity/skills dissoc id)
       nil)

   :tx/set-item (fn [_ctx eid cell item]
                  (let [entity @eid
                        inventory (:entity/inventory entity)]
                    (assert (and (nil? (get-in inventory cell))
                                 (inventory/valid-slot? cell item)))
                    (swap! eid assoc-in (cons :entity/inventory cell) item)
                    (when (inventory/applies-modifiers? cell)
                      (swap! eid update :entity/stats stats/add (:stats/modifiers item)))
                    nil))

   :tx/remove-item (fn [_ctx eid cell]
                     (let [entity @eid
                           item (get-in (:entity/inventory entity) cell)]
                       (assert item)
                       (swap! eid assoc-in (cons :entity/inventory cell) nil)
                       (when (inventory/applies-modifiers? cell)
                         (swap! eid update :entity/stats stats/remove-mods (:stats/modifiers item)))
                       nil))

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
   :tx/event (fn [{:keys [ctx/world]} & params]
               (apply handle-event world params))
   :tx/state-exit (fn [ctx eid [state-k state-v]]
                    (state/exit [state-k state-v] eid ctx))
   :tx/state-enter (fn [_ctx eid [state-k state-v]]
                     (state/enter [state-k state-v] eid))
   :tx/effect (fn [{:keys [ctx/world]} effect-ctx effects]
                (mapcat #(effect/handle % effect-ctx world)
                        (filter #(effect/applicable? % effect-ctx) effects)))
   :tx/audiovisual (fn
                     [{:keys [ctx/db]} position audiovisual]
                     (let [{:keys [tx/sound
                                   entity/animation]} (if (keyword? audiovisual)
                                                        (db/build db audiovisual)
                                                        audiovisual)]
                       [[:tx/sound sound]
                        [:tx/spawn-effect
                         position
                         {:entity/animation (assoc animation :delete-after-stopped? true)}]]))
   :tx/spawn-alert (fn [{:keys [ctx/world]} position faction duration]
                     [[:tx/spawn-effect
                       position
                       {:entity/alert-friendlies-after-duration
                        {:counter (timer/create (:world/elapsed-time world) duration)
                         :faction faction}}]])
   :tx/spawn-line (fn [_ctx {:keys [start end duration color thick?]}]
                    [[:tx/spawn-effect
                      start
                      {:entity/line-render {:thick? thick? :end end :color color}
                       :entity/delete-after-duration duration}]])
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
   :tx/spawn-projectile (fn [_ctx
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
   :tx/spawn-effect (fn [{:keys [ctx/world]}
                         position
                         components]
                      [[:tx/spawn-entity
                        (assoc components
                               :entity/body (assoc (:world/effect-body-props world) :position position))]])
   :tx/spawn-item     (fn [_ctx position item]
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
   :tx/spawn-creature (fn [_ctx
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
   :tx/spawn-entity   (fn [{:keys [ctx/world]} entity]
                        (let [{:keys [world/content-grid
                                      world/entity-ids
                                      world/grid
                                      world/id-counter
                                      world/spawn-entity-schema]} world
                              _ (mu/validate-humanize spawn-entity-schema entity)
                              entity (reduce (fn [m [k v]]
                                               (assoc m k (create-component [k v] world)))
                                             {}
                                             entity)
                              _ (assert (and (not (contains? entity :entity/id))))
                              entity (assoc entity :entity/id (swap! id-counter inc))
                              entity (merge (map->Entity {}) entity)
                              eid (atom entity)]
                          (let [id (:entity/id @eid)]
                            (assert (number? id))
                            (swap! entity-ids assoc id eid))
                          (content-grid/add-entity! content-grid eid)
                          ; https://github.com/damn/core/issues/58
                          ;(assert (valid-position? grid @eid))
                          (grid/set-touched-cells! grid eid)
                          (when (:body/collides? (:entity/body @eid))
                            (grid/set-occupied-cells! grid eid))
                          (mapcat #(after-create-component % eid world) @eid)))

   :tx/sound (fn [{:keys [ctx/audio]} sound-name]
               (audio/play! audio sound-name)
               nil)
   :tx/toggle-inventory-visible toggle-inventory-visible!
   :tx/show-message             show-message!
   :tx/show-modal               show-modal!
   }
  )

(def ^:private reaction-txs-fn-map
  {

   :tx/set-item (fn [ctx eid cell item]
                  (when (:entity/player? @eid)
                    (player-set-item! ctx cell item)
                    nil))

   :tx/remove-item (fn [ctx eid cell]
                     (when (:entity/player? @eid)
                       (player-remove-item! ctx cell)
                       nil))

   :tx/add-skill (fn [ctx eid skill]
                   (when (:entity/player? @eid)
                     (player-add-skill! ctx skill)
                     nil))
   }
  )

(extend-type Context
  txs/TransactionHandler
  (handle! [ctx txs]
    (let [handled-txs (tx-handler/actions! txs-fn-map
                                           ctx
                                           txs)]
      (tx-handler/actions! reaction-txs-fn-map
                           ctx
                           handled-txs
                           :strict? false))))

(defn- handle-files
  [files {:keys [colors
                 cursors
                 default-font
                 tile-size
                 texture-folder
                 ui-viewport
                 world-viewport]}]
  {:ui-viewport ui-viewport
   :default-font {:file-handle (Files/.internal files (:path default-font))
                  :params (:params default-font)}
   :colors colors
   :cursors (update-vals (:data cursors)
                         (fn [[short-path hotspot]]
                           [(Files/.internal files (format (:path-format cursors) short-path))
                            hotspot]))
   :world-unit-scale (float (/ tile-size))
   :world-viewport world-viewport
   :textures-to-load (files/search files texture-folder)})

(defn create-graphics
  [ctx
   params]
  (assoc ctx :ctx/graphics (cdq.graphics.impl/create! (handle-files Gdx/files params)
                                                      Gdx/graphics)))

(defn create-input [{:keys [ctx/stage]
                     :as ctx}]
  (let [input Gdx/input]
    (.setInputProcessor input stage)
    (assoc ctx :ctx/input input)))

(defn- create! []
  (-> {}
      map->Context
      (assoc :ctx/db (cdq.impl.db/create))
      (create-graphics {:tile-size 48
                        :ui-viewport {:width 1440
                                      :height 900}
                        :world-viewport {:width 1440
                                         :height 900}
                        :texture-folder {:folder "resources/"
                                         :extensions #{"png" "bmp"}}
                        :default-font {:path "exocet/films.EXL_____.ttf"
                                       :params {:size 16
                                                :quality-scaling 2
                                                :enable-markup? true
                                                :use-integer-positions? false
                                                ; :texture-filter/linear because scaling to world-units
                                                :min-filter :linear
                                                :mag-filter :linear}}
                        :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
                        :cursors {:path-format "cursors/%s.png"
                                  :data {:cursors/bag                   ["bag001"       [0   0]]
                                         :cursors/black-x               ["black_x"      [0   0]]
                                         :cursors/default               ["default"      [0   0]]
                                         :cursors/denied                ["denied"       [16 16]]
                                         :cursors/hand-before-grab      ["hand004"      [4  16]]
                                         :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                         :cursors/hand-grab             ["hand003"      [4  16]]
                                         :cursors/move-window           ["move002"      [16 16]]
                                         :cursors/no-skill-selected     ["denied003"    [0   0]]
                                         :cursors/over-button           ["hand002"      [0   0]]
                                         :cursors/sandclock             ["sandclock"    [16 16]]
                                         :cursors/skill-not-usable      ["x007"         [0   0]]
                                         :cursors/use-skill             ["pointer004"   [0   0]]
                                         :cursors/walking               ["walking"      [16 16]]}}})
      (ui/create! '[[cdq.ctx.create.ui.dev-menu/create cdq.application/create-world]
                    [cdq.ctx.create.ui.action-bar/create]
                    [cdq.ctx.create.ui.hp-mana-bar/create]
                    [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                       [cdq.ctx.create.ui.windows.inventory/create]]]
                    [cdq.ctx.create.ui.player-state-draw/create]
                    [cdq.ctx.create.ui.message/create]])
      create-input
      (assoc :ctx/audio (audio/create Gdx/audio (files/sound-names->file-handles Gdx/files)))
      (create-world "world_fns/vampire.edn")))

(defn- resize! [{:keys [ctx/graphics]} width height]
  (ui-viewport/update!    graphics width height)
  (world-viewport/update! graphics width height))

(defn- dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose!)
  (world/dispose! world))

(defn- get-stage-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (or (ui/get-ctx stage)
      ctx)) ; first render stage does not have ctx set.

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (world-viewport/unproject graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position   ] (ui-viewport/unproject    graphics mp))
        )))

(defn- get-mouseover-entity
  [{:keys [world/grid
           world/mouseover-eid
           world/player-eid
           world/render-z-order]
    :as world}
   position]
  (let [player @player-eid
        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                     (grid/point->entities grid position))]
    (->> render-z-order
         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
         reverse
         (filter #(raycaster/line-of-sight? world player @%))
         first)))

(defn- update-mouseover-eid
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))
        mouseover-eid (:world/mouseover-eid world)
        new-eid (if mouseover-actor
                  nil
                  (get-mouseover-entity world (:graphics/world-mouse-position graphics)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(defn- check-open-debug
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (camera/set-position! graphics
                        (:body/position
                         (:entity/body
                          @(:world/player-eid world))))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
  ctx)

(defn- tile-color-setter
  [{:keys [ray-blocked?
           explored-tile-corners
           light-position
           see-all-tiles?
           explored-tile-color
           visible-tile-color
           invisible-tile-color]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         invisible-tile-color)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (ray-blocked? light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            visible-tile-color
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              visible-tile-color))))))

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

(defn draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (tiled-map-renderer/draw! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial raycaster/blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (camera/position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(def ^:dbg-flag show-tile-grid? false)

(defn draw-tile-grid
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (camera/frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (world-viewport/width  graphics)))
        (+ 2 (int (world-viewport/height graphics)))
        1
        1
        [1 1 1 0.8]]])))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn draw-cell-debug
  [{:keys [ctx/graphics
           ctx/world]}]
  (apply concat
         (for [[x y] (camera/visible-tiles graphics)
               :let [cell ((:world/grid world) [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn geom-test
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [position (:graphics/world-mouse-position graphics)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells (:world/grid world) circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [[x y] (mapv int (:graphics/world-mouse-position graphics))
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx} ]
  (world-viewport/draw! graphics
                        (fn []
                          (doseq [f [draw-tile-grid
                                     draw-cell-debug
                                     draw-entities
                                     #_geom-test
                                     highlight-mouseover-tile]]
                            (draws/handle! graphics (f ctx)))))
  ctx)

(defn set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)] eid ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn player-state-handle-input
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)

(defn dissoc-interaction-state [ctx]
  (dissoc ctx :ctx/interaction-state))

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (input/unpause? input))))))

(defn- update-world-time* [{:keys [world/max-delta]
                           :as world}
                          delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn update-world-time
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world update-world-time* (graphics/delta-time graphics))))

(defn update-potential-fields
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))

(defn- render! [ctx]
  (-> ctx
      get-stage-ctx
      validate
      update-mouse
      update-mouseover-eid
      check-open-debug
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      assoc-interaction-state
      set-cursor!
      player-state-handle-input
      dissoc-interaction-state
      assoc-paused
      update-world-time
      update-potential-fields
      tick-entities
      remove-destroyed-entities
      window-camera-controls
      render-stage
      validate))

(def state (atom nil))

(defn -main []
  (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
  (Lwjgl3Application. (reify ApplicationListener
                        (create [_]
                          (reset! state (create!)))
                        (dispose [_]
                          (dispose! @state))
                        (render [_]
                          (swap! state render!))
                        (resize [_ width height]
                          (resize! @state width height))
                        (pause [_])
                        (resume [_]))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Cyber Dungeon Quest")
                        (.setWindowedMode 1440 900)
                        (.setForegroundFPS 60))))

(comment

 (.postRunnable Gdx/app
                (fn []
                  (let [{:keys [ctx/db]
                         :as ctx} @state]
                    (txs/handle! ctx
                                 [[:tx/spawn-creature
                                   {:position [35 73]
                                    :creature-property (db/build db :creatures/dragon-red)
                                    :components {:entity/fsm {:fsm :fsms/npc
                                                              :initial-state :npc-sleeping}
                                                 :entity/faction :evil}}]]))))
 )
