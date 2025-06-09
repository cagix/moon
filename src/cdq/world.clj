(ns cdq.world
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.grid2d :as g2d]
            [cdq.grid-impl :as grid-impl]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.utils :as utils]))

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

(defn spawn-creature! [ctx
                       {:keys [position
                               creature-property
                               components]}]
  (assert creature-property)
  (let [props creature-property]
    (ctx/spawn-entity! ctx
                       position
                       (create-creature-body (:entity/body props))
                       (-> props
                           (dissoc :entity/body)
                           (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                           (utils/safe-merge components)))))

(defn create [ctx config {:keys [tiled-map
                                 start-position
                                 creatures
                                 player-entity]}]
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
        max-speed (/ minimum-size max-delta)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map ; only @ cdq.render.draw-world-map -> pass graphics??
                    :ctx/elapsed-time 0 ; -> everywhere
                    :ctx/grid grid ; -> everywhere -> abstract ?
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create (:tiled-map/width  tiled-map)
                                                           (:tiled-map/height tiled-map)
                                                           (:content-grid-cell-size config))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                                                                      (:tiled-map/height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)
                    :ctx/factions-iterations (:potential-field-factions-iterations config)
                    :ctx/z-orders z-orders
                    :ctx/render-z-order (utils/define-order z-orders)
                    :ctx/minimum-size minimum-size
                    :ctx/max-delta max-delta
                    :ctx/max-speed max-speed})
        ctx (assoc ctx :ctx/player-eid (spawn-creature! ctx player-entity))]
    (run! (partial spawn-creature! ctx) creatures)
    ctx))

(defn calculate-active-entities [{:keys [ctx/content-grid
                                         ctx/player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn update-potential-fields!
  [{:keys [ctx/potential-field-cache
           ctx/factions-iterations
           ctx/grid
           ctx/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))
