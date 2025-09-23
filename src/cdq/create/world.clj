(ns cdq.create.world
  (:require cdq.impl.content-grid
            cdq.impl.grid
            cdq.potential-fields.movement
            [cdq.world.grid.cell :as cell]
            [cdq.world]
            [clojure.math.vector2 :as v]
            [clojure.grid2d :as g2d]
            [clojure.disposable :as disposable]
            [clojure.tiled :as tiled]
            [clojure.utils :as utils]
            [malli.core :as m]
            [reduce-fsm :as fsm])
  (:import (cdq.math RayCaster)))

(defn- blocked? [[arr width height] [start-x start-y] [target-x target-y]]
  (RayCaster/rayBlocked (double start-x)
                        (double start-y)
                        (double target-x)
                        (double target-y)
                        width
                        height
                        arr))

(defn- create-double-ray-endpositions
  [[start-x start-y]
   [target-x target-y]
   path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- path-blocked? [raycaster start target path-w]
  (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
    (or
     (blocked? raycaster start1 target1)
     (blocked? raycaster start2 target2))))

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

(defrecord World []
  cdq.world/RayCaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (path-blocked? raycaster start target path-w))

  (line-of-sight? [{:keys [world/raycaster]} source target]
    (not (blocked? raycaster
                   (:body/position (:entity/body source))
                   (:body/position (:entity/body target)))))

  cdq.world/MovementAI
  (find-movement-direction [{:keys [world/grid]} eid]
    (cdq.potential-fields.movement/find-movement-direction grid eid))

  disposable/Disposable
  (dispose! [{:keys [world/tiled-map]}]
    (when tiled-map ; initialization
      (disposable/dispose! tiled-map)))

  cdq.world/World
  (active-eids [this]
    (:world/active-entities this))

  cdq.world/Resettable
  (reset-state [world {:keys [tiled-map
                              start-position]}]
    (let [width  (:tiled-map/width  tiled-map)
          height (:tiled-map/height tiled-map)
          grid (cdq.impl.grid/create width height
                                     #(case (tiled/movement-property tiled-map %)
                                        "none" :none
                                        "air"  :air
                                        "all"  :all))]
      (assoc world
             :world/tiled-map tiled-map
             :world/start-position start-position
             :world/grid grid
             :world/content-grid (cdq.impl.content-grid/create width height (:content-grid-cell-size world))
             :world/explored-tile-corners (create-explored-tile-corners width height)
             :world/raycaster (create-raycaster grid)
             :world/elapsed-time 0
             :world/potential-field-cache (atom nil)
             :world/id-counter (atom 0)
             :world/entity-ids (atom {})
             :world/paused? false
             :world/mouseover-eid nil))))

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

(defn- create-record [world]
  (merge (map->World {}) world))

(defn- entity-schema [world]
  (assoc world :world/spawn-entity-schema components-schema))

(defn do! [ctx {:keys [initial-state]}]
  (assoc ctx :ctx/world (-> initial-state
                            create-record
                            entity-schema
                            create-fsms
                            calculate-max-speed
                            define-render-z-order)))
