(ns cdq.application.create.world
  (:require [cdq.world]
            [cdq.grid.cell :as cell]
            [clojure.grid2d :as g2d]
            [cdq.world.raycaster :as raycaster]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [gdl.tiled :as tiled]
            [clojure.utils :as utils]
            [reduce-fsm :as fsm]))

(defn- create-explored-tile-corners [width height]
  (atom (g2d/create-grid width height (constantly false))))

(defn- create-raycaster [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (let [arr (make-array Boolean/TYPE width height)]
      (doseq [[[x y] blocked?] cells]
        (aset arr x y (boolean blocked?)))
      [arr width height])))

(comment

 ; 1. quote the data structur ebecause of arrows
 ; 2.
 (eval `(fsm/fsm-inc ~data))
 )

(def ^:private npc-fsm
  (fsm/fsm-inc
   [[:npc-sleeping
     :kill -> :npc-dead
     :stun -> :stunned
     :alert -> :npc-idle]
    [:npc-idle
     :kill -> :npc-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :movement-direction -> :npc-moving]
    [:npc-moving
     :kill -> :npc-dead
     :stun -> :stunned
     :timer-finished -> :npc-idle]
    [:active-skill
     :kill -> :npc-dead
     :stun -> :stunned
     :action-done -> :npc-idle]
    [:stunned
     :kill -> :npc-dead
     :effect-wears-off -> :npc-idle]
    [:npc-dead]]))

(def ^:private player-fsm
  (fsm/fsm-inc
   [[:player-idle
     :kill -> :player-dead
     :stun -> :stunned
     :start-action -> :active-skill
     :pickup-item -> :player-item-on-cursor
     :movement-input -> :player-moving]
    [:player-moving
     :kill -> :player-dead
     :stun -> :stunned
     :no-movement-input -> :player-idle]
    [:active-skill
     :kill -> :player-dead
     :stun -> :stunned
     :action-done -> :player-idle]
    [:stunned
     :kill -> :player-dead
     :effect-wears-off -> :player-idle]
    [:player-item-on-cursor
     :kill -> :player-dead
     :stun -> :stunned
     :drop-item -> :player-idle
     :dropped-item -> :player-idle]
    [:player-dead]]))

(defrecord World []
  cdq.world/RayCaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (raycaster/blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (raycaster/path-blocked? raycaster start target path-w))

  (line-of-sight? [{:keys [world/raycaster]} source target]
    (not (raycaster/blocked? raycaster
                             (:body/position (:entity/body source))
                             (:body/position (:entity/body target)))))

  cdq.world/MovementAI
  (find-movement-direction [{:keys [world/grid
                                    world/movement-ai]} eid]
    (movement-ai grid eid))

  cdq.world/World
  (dispose! [{:keys [world/tiled-map]}]
    (disposable/dispose! tiled-map))

  (active-eids [this]
    (:world/active-entities this))

  cdq.world/Resettable
  (reset-state [world {:keys [tiled-map
                              start-position]}]
    (let [width  (:tiled-map/width  tiled-map)
          height (:tiled-map/height tiled-map)
          create-grid (requiring-resolve 'cdq.impl.grid/create)
          grid (create-grid width height
                            #(case (tiled/movement-property tiled-map %)
                               "none" :none
                               "air"  :air
                               "all"  :all))
          create-content-grid (requiring-resolve 'cdq.impl.content-grid/create)]
      (assoc world
             :world/tiled-map tiled-map
             :world/start-position start-position
             :world/grid grid
             :world/content-grid (create-content-grid width height (:content-grid-cell-size world))
             :world/explored-tile-corners (create-explored-tile-corners width height)
             :world/raycaster (create-raycaster grid)
             :world/elapsed-time 0
             :world/potential-field-cache (atom nil)
             :world/id-counter (atom 0)
             :world/entity-ids (atom {})
             :world/paused? false
             :world/mouseover-eid nil))))

(defn- calculate-max-speed
  [{:keys [world/minimum-size
           world/max-delta]
    :as world}]
  (assoc world :world/max-speed (/ minimum-size max-delta)))

(defn- define-render-z-order
  [{:keys [world/z-orders]
    :as world}]
  (assoc world :world/render-z-order (utils/define-order z-orders)))

(defn- create-fsms
  [world]
  (assoc world :world/fsms {:fsms/player player-fsm
                            :fsms/npc npc-fsm}))

(defn do! [ctx {:keys [initial-state
                       pipeline]}]
  (assoc ctx :ctx/world (utils/pipeline initial-state pipeline)))
