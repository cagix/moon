(ns cdq.application.create.world.protocols
  (:require [cdq.grid.cell :as cell]
            [cdq.world]
            [cdq.world.raycaster :as raycaster]
            [clojure.grid2d :as g2d]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [gdl.tiled :as tiled]))

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

(defn do! [world]
  (extend-type (class world)
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
  world)
