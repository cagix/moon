(ns cdq.game.create
  (:require [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.files :as files]
            [cdq.graphics.textures :as textures]
            [cdq.ui :as ui]
            [cdq.world.info :as info]
            cdq.impl.db
            cdq.impl.graphics
            cdq.impl.ui
            cdq.impl.world
            [cdq.effect :as effect]
            [cdq.world.content-grid :as content-grid]
            [clojure.math.vector2 :as v]
            [cdq.world.grid :as grid]
            cdq.entity.animation
            cdq.entity.body
            [cdq.entity.skills :as skills]
            [cdq.entity.state :as state]
            [clojure.timer :as timer]
            [cdq.entity.inventory :as inventory]
            cdq.entity.delete-after-duration
            cdq.entity.projectile-collision
            cdq.entity.fsm
            [cdq.entity.stats :as stats]
            [cdq.world-fns.creature-tiles]
            [cdq.ui :as ui]
            [clojure.utils :as utils]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.txs :as txs]
            [clojure.tx-handler :as tx-handler]
            [qrecord.core :as q]
            [malli.utils :as mu]
            [reduce-fsm :as fsm])
  (:import (cdq.ui Stage)
           (com.badlogic.gdx Files)))

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

(q/defrecord Context []
  txs/TransactionHandler
  (handle! [ctx txs]
    (let [handled-txs (tx-handler/actions! txs-fn-map
                                           ctx
                                           txs)]
      (tx-handler/actions! reaction-txs-fn-map
                           ctx
                           handled-txs
                           :strict? false))))

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

(def graphics-params
  {:tile-size 48
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

(defn create-input [{:keys [ctx/stage]
                     :as ctx} input]
  (.setInputProcessor input stage)
  (assoc ctx :ctx/input input))

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(defn do! [{:keys [files graphics input]
            :as gdx}]
  (-> {}
      map->Context
      (assoc :ctx/db (cdq.impl.db/create))
      (assoc :ctx/graphics (cdq.impl.graphics/create! (handle-files files graphics-params)
                                                      graphics
                                                      gdx))
      (ui/create! '[[cdq.ctx.create.ui.dev-menu/create cdq.game.create/create-world]
                    [cdq.ctx.create.ui.action-bar/create]
                    [cdq.ctx.create.ui.hp-mana-bar/create]
                    [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                       [cdq.ctx.create.ui.windows.inventory/create]]]
                    [cdq.ctx.create.ui.player-state-draw/create]
                    [cdq.ctx.create.ui.message/create]])
      (create-input input)
      (assoc :ctx/audio (audio/create gdx sound-names path-format))
      (create-world "world_fns/vampire.edn")))
