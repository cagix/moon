(ns cdq.ctx.game
  (:require [cdq.content-grid :as content-grid]
            [cdq.cell :as cell]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            cdq.entity.animation
            cdq.entity.body
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.destroy-audiovisual
            cdq.entity.fsm
            cdq.entity.inventory
            cdq.entity.projectile-collision
            cdq.entity.skills
            cdq.entity.stats
            [cdq.grid2d :as g2d]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.math.geom :as geom]
            [cdq.modifiers :as modifiers]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.w :as w]
            [cdq.malli :as m]
            [cdq.math.vector2 :as v]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.ui.player-state-draw
            cdq.entity.state.player-item-on-cursor
            cdq.ui.message
            [cdq.ui.stage :as stage]
            [qrecord.core :as q]))

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ;graphics db
                           {:world-fns '[[cdq.level.from-tmx/create {:tmx-file "maps/vampire.tmx"
                                                                     :start-position [32 71]}]
                                         [cdq.level.uf-caves/create {:tile-size 48
                                                                     :texture "maps/uf_terrain.png"
                                                                     :spawn-rate 0.02
                                                                     :scaling 3
                                                                     :cave-size 200
                                                                     :cave-style :wide}]
                                         [cdq.level.modules/create {:world/map-size 5,
                                                                    :world/max-area-level 3,
                                                                    :world/spawn-rate 0.05}]]
                            ;icons, etc. , components ....
                            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"})
    (cdq.ui.action-bar/create {:id :action-bar}) ; padding.... !, etc.

    ; graphics
    (cdq.ui.hp-mana-bar/create ctx
                               {:rahmen-file "images/rahmen.png"
                                :rahmenw 150
                                :rahmenh 26
                                :hpcontent-file "images/hp.png"
                                :manacontent-file "images/mana.png"
                                :y-mana 80}) ; action-bar-icon-size

    {:actor/type :actor.type/group
     :id :windows
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0}) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :state->clicked-inventory-cell
                {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
                 :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-cell}})]}
    (cdq.ui.player-state-draw/create
     {:state->draw-gui-view
      {:player-item-on-cursor
       cdq.entity.state.player-item-on-cursor/draw-gui-view}})
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])

(defn- reset-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (doseq [actor (create-ui-actors ctx)]
    (stage/add! stage actor))
  ctx)

(defn- context-entity-add! [{:keys [world/entity-ids
                                    world/content-grid
                                    world/grid]}
                            eid]
  (let [id (entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid))
  (grid/add-entity! grid eid))

(defn- context-entity-remove! [{:keys [world/entity-ids
                                       world/grid]}
                               eid]
  (let [id (entity/id @eid)]
    (assert (contains? @entity-ids id))
    (swap! entity-ids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity! grid eid))

(defn- context-entity-moved! [{:keys [world/content-grid
                                      world/grid]}
                              eid]
  (content-grid/position-changed! content-grid eid)
  (grid/position-changed! grid eid))

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

(q/defrecord Entity [
                     entity/body
                     ]
  entity/Entity
  (position [_]
    (:body/position body))

  (rectangle [_]
    (geom/body->gdx-rectangle body))

  (overlaps? [this other-entity]
    (geom/overlaps? (geom/body->gdx-rectangle (:entity/body this))
                    (geom/body->gdx-rectangle (:entity/body other-entity))))

  ; body-fn
  (in-range? [entity target* maxrange]
    (< (- (float (v/distance (entity/position entity)
                             (entity/position target*)))
          (float (/ (:body/width (:entity/body entity))  2))
          (float (/ (:body/width (:entity/body target*)) 2)))
       (float maxrange)))

  (id [{:keys [entity/id]}]
    id)

  (faction [{:keys [entity/faction]}]
    faction)

  (enemy [this]
    (case (entity/faction this)
      :evil :good
      :good :evil))

  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (modifiers/not-enough-mana? (:creature/stats entity) skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable))

  (mod-add    [entity mods] (update entity :creature/stats modifiers/add    mods))
  (mod-remove [entity mods] (update entity :creature/stats modifiers/remove mods))

  (stat [this k]
    (modifiers/get-stat-value (:creature/stats this) k))

  (mana [entity]
    (modifiers/get-mana (:creature/stats entity)))

  (mana-val [entity]
    (modifiers/mana-val (:creature/stats entity)))

  (hitpoints [entity]
    (modifiers/get-hitpoints (:creature/stats entity)))

  (pay-mana-cost [entity cost]
    (update entity :creature/stats modifiers/pay-mana-cost cost)))

(def ^:private entity-components
  {:entity/animation                       {:create   cdq.entity.animation/create}
   :entity/body                            {:create   cdq.entity.body/create}
   :entity/delete-after-animation-stopped? {:create!  cdq.entity.delete-after-animation-stopped/create!}
   :entity/delete-after-duration           {:create   cdq.entity.delete-after-duration/create}
   :entity/projectile-collision            {:create   cdq.entity.projectile-collision/create}
   :creature/stats                         {:create   cdq.entity.stats/create}
   :entity/fsm                             {:create!  cdq.entity.fsm/create!}
   :entity/inventory                       {:create!  cdq.entity.inventory/create!}
   :entity/skills                          {:create!  cdq.entity.skills/create!}
   :entity/destroy-audiovisual             {:destroy! cdq.entity.destroy-audiovisual/destroy!}})

(defn- create-component-value
  [world k v]
  (if-let [create (:create (k entity-components))]
    (create v world)
    v))

(defn- create!-component-value
  [world [k v] eid]
  (when-let [create! (:create! (k entity-components))]
    (create! v eid world)))

(defn- component-destroy!
  [world [k v] eid]
  (when-let [destroy! (:destroy! (k entity-components))]
    (destroy! v eid world)))

(defn- create-vs [components world]
  (reduce (fn [m [k v]]
            (assoc m k (create-component-value world k v)))
          {}
          components))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- create-creature-body [position {:keys [body/width body/height #_body/flying?]}]
  {:position position
   :width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(q/defrecord World [
                    world/tiled-map
                    world/start-position
                    world/grid
                    world/explored-tile-corners
                    world/content-grid
                    world/raycaster
                    world/entity-states
                    world/potential-field-cache
                    world/factions-iterations
                    world/id-counter
                    world/entity-ids
                    world/elapsed-time
                    world/max-delta
                    world/max-speed
                    world/minimum-size
                    world/z-orders
                    world/render-z-order

                    ; added later
                    world/delta-time
                    world/paused?
                    world/active-entities
                    world/mouseover-eid
                    world/player-eid
                    ;
                    ]
  w/World
  (spawn-entity!
    [{:keys [world/id-counter]
      :as world}
     components]
    (m/validate-humanize components-schema components)
    (assert (and (not (contains? components :entity/id))))
    (let [eid (atom (merge (map->Entity {})
                           (-> components
                               (assoc :entity/id (swap! id-counter inc))
                               (create-vs world))))]
      (context-entity-add! world eid)
      (mapcat #(create!-component-value world % eid) @eid)))

  (spawn-creature! [world
                    {:keys [position
                            creature-property
                            components]}]
    (assert creature-property)
    (w/spawn-entity! world
                     (-> creature-property
                         (assoc :entity/body (create-creature-body position
                                                                   (:entity/body creature-property)))
                         (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                         (utils/safe-merge components))))

  (remove-entity! [world eid]
    (context-entity-remove! world eid)
    (mapcat #(component-destroy! world % eid) @eid))

  (move-entity! [world eid body direction rotate-in-movement-direction?]
    (context-entity-moved! world eid)
    (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
    (when rotate-in-movement-direction?
      (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
    nil)

  (line-of-sight? [{:keys [world/raycaster]}
                   source
                   target]
    (assert raycaster)
    (not (raycaster/blocked? raycaster
                             (entity/position source)
                             (entity/position target))))

  (nearest-enemy-distance [{:keys [world/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [world/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [world/grid]} eid]
    (potential-fields.movement/find-direction grid eid))

  (creatures-in-los-of-player
    [{:keys [world/active-entities
             world/player-eid]
      :as world}]
    (->> active-entities
         (filter #(:entity/species @%))
         (filter #(w/line-of-sight? world @player-eid @%))
         (remove #(:entity/player? @%))))

  (npc-effect-ctx [world eid]
    (let [entity @eid
          target (w/nearest-enemy world entity)
          target (when (and target
                            (w/line-of-sight? world entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (entity/position entity)
                                               (entity/position @target)))}))

  (path-blocked? [{:keys [world/raycaster]} start end width]
    (raycaster/path-blocked? raycaster start end width))

  (update-time [{:keys [world/max-delta] :as world} delta-ms]
    (let [delta-ms (min delta-ms max-delta)]
      (-> world
          (assoc :world/delta-time delta-ms)
          (update :world/elapsed-time + delta-ms)))))

(defn- create-world
  [{:keys [tiled-map
           start-position]
    :as config}]
  (let [grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)]
    (merge (map->World {})
           {:world/tiled-map tiled-map
            :world/start-position start-position
            :world/grid grid
            :world/explored-tile-corners (create-explored-tile-corners tiled-map)
            :world/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                     (:tiled-map/height tiled-map)
                                                     (:content-grid-cell-size config))
            :world/raycaster (raycaster/create grid)
            :world/entity-states (:entity-states config)
            :world/potential-field-cache (atom nil)
            :world/factions-iterations (:potential-field-factions-iterations config)
            :world/id-counter (atom 0)
            :world/entity-ids (atom {})
            :world/elapsed-time 0
            :world/max-delta max-delta
            :world/max-speed max-speed
            :world/minimum-size minimum-size
            :world/z-orders z-orders
            :world/render-z-order (utils/define-order z-orders)})))

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (create-world (merge (:cdq.ctx.game/world config)
                                             (let [[f params] world-fn]
                                               (f ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (w/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc-in ctx [:ctx/world :world/player-eid] player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (w/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

(defn reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))
