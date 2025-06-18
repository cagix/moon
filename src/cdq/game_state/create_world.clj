(ns cdq.game-state.create-world
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.malli :as m]
            [cdq.math.geom :as geom]
            [cdq.modifiers :as modifiers]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.utils :as utils]
            [cdq.w :as w]
            [gdl.math.vector2 :as v]
            [qrecord.core :as q]
            [master.yoda :as yoda]))

(defn- context-entity-add! [{:keys [world/entity-ids
                                    world/content-grid
                                    world/grid]}
                            eid]
  (let [id (entity/id @eid)]
    (assert (number? id))
    (swap! entity-ids assoc id eid))
  (content-grid/add-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
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

(q/defrecord Body [entity/position
                   left-bottom

                   width
                   height
                   half-width
                   half-height
                   radius

                   collides?
                   z-order
                   rotation-angle]
  entity/Entity
  (position [_]
    position)

  (rectangle [_]
    (let [[x y] left-bottom]
      (geom/rectangle x y width height)))

  (overlaps? [this other-entity]
    (geom/overlaps? (entity/rectangle this)
                    (entity/rectangle other-entity)))

  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (entity/position entity)
                             (entity/position target*)))
          (float (:radius entity))
          (float (:radius target*)))
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

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}
                    minimum-size
                    z-orders]
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

    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]

    :width  (float width)
    :height (float height)

    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))

    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-component-value
  [world k v]
  (if-let [create (:create (k (:world/entity-components world)))]
    (create v world)
    v))

(defn- create!-component-value
  [world [k v] eid]
  (when-let [create! (:create! (k (:world/entity-components world)))]
    (create! v eid world)))

(defn- component-destroy!
  [world [k v] eid]
  (when-let [destroy! (:destroy! (k (:world/entity-components world)))]
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
(defn- create-creature-body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
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
                    world/entity-components
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
    [{:keys [world/minimum-size
             world/z-orders
             world/id-counter]
      :as world}
     position
     body
     components]
    (m/validate-humanize components-schema components)
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        (create-body minimum-size z-orders)
                        (utils/safe-merge (-> components
                                              (assoc :entity/id (swap! id-counter inc))
                                              (create-vs world)))))]
      (context-entity-add! world eid)
      (mapcat #(create!-component-value world % eid) @eid)))

  (spawn-creature! [world
                    {:keys [position
                            creature-property
                            components]}]
    (assert creature-property)
    (let [props creature-property]
      (w/spawn-entity! world
                       position
                       (create-creature-body (:entity/body props))
                       (-> props
                           (dissoc :entity/body)
                           (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                           (utils/safe-merge components)))))

  (remove-entity! [world eid]
    (context-entity-remove! world eid)
    (mapcat #(component-destroy! world % eid) @eid))

  (move-entity! [world eid body direction rotate-in-movement-direction?]
    (context-entity-moved! world eid)
    (swap! eid assoc
           :entity/position (:entity/position body)
           :left-bottom     (:left-bottom     body))
    (when rotate-in-movement-direction?
      (swap! eid assoc :rotation-angle (v/angle-from-vector direction)))
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

(defn create-world
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
            :world/entity-components (:entity-components config)
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

(defn do!
  [{:keys [ctx/config]
    :as ctx}
   pipeline]
  (let [level (let [[f params] (:config/starting-world config)]
                (f ctx params))]
    (assoc ctx :ctx/world
           (reduce yoda/render*
                   (merge (:cdq.ctx.game/world config)
                          level)
                   pipeline))))
