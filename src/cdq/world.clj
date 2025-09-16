(ns cdq.world
  (:require [cdq.math.raycaster :as raycaster]
            [cdq.math.path-rays :as path-rays]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.explored-tile-corners :as explored-tile-corners]
            [cdq.world.grid :as grid]
            [cdq.world.raycaster]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.tiled :as tiled]))

(defn reset-state
  [world
   {:keys [tiled-map
           start-position]}]
  (let [width  (:tiled-map/width  tiled-map)
        height (:tiled-map/height tiled-map)
        grid (grid/create width height
                          #(case (tiled/movement-property tiled-map %)
                             "none" :none
                             "air"  :air
                             "all"  :all))]
    (assoc world
           :world/tiled-map tiled-map
           :world/start-position start-position
           :world/grid grid
           :world/content-grid (content-grid/create width height (:content-grid-cell-size world))
           :world/explored-tile-corners (explored-tile-corners/create width height)
           :world/raycaster (cdq.world.raycaster/create grid)
           :world/elapsed-time 0
           :world/potential-field-cache (atom nil)
           :world/id-counter (atom 0)
           :world/entity-ids (atom {})
           :world/paused? false
           :world/mouseover-eid nil)))

(def active-eids :world/active-entities)

(defn dispose! [{:keys [world/tiled-map]}]
  (disposable/dispose! tiled-map))

(defn path-blocked? [{:keys [world/raycaster]} start target path-w]
  (let [[start1,target1,start2,target2] (path-rays/create-double-ray-endpositions start target path-w)]
    (or
     (raycaster/blocked? raycaster start1 target1)
     (raycaster/blocked? raycaster start2 target2))))

(defn line-of-sight? [{:keys [world/raycaster]} source target]
  (not (raycaster/blocked? raycaster
                           (:body/position (:entity/body source))
                           (:body/position (:entity/body target)))))

(defn find-movement-direction [{:keys [world/grid]} eid]
  (potential-fields.movement/find-direction grid eid))
