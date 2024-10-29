(ns moon.world.start
  (:require [data.grid2d :as g2d]
            [gdl.tiled :as tiled]
            [moon.component :as component]
            [moon.world.grid :as grid]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.world :as world]
            [moon.world.content-grid :as content-grid]
            [moon.world.entities :as entities]
            [moon.world.grid :as grid]
            [moon.world.raycaster :as raycaster]
            [moon.world.time :as time]))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/width tiled-map)
   (tiled/height tiled-map)
   (fn [position]
     (atom (grid/->cell position
                        (case (level/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

(defc :world/start
  (component/handle [[_ world-id]]
    (screen/change :screens/world)
    (stage/reset (component/create [:world/widgets]))
    (let [{:keys [tiled-map] :as level} (level/generate world-id)]
      (world/clear-tiled-map)
      (bind-root #'world/tiled-map tiled-map)
      (bind-root #'grid/grid (create-grid tiled-map))
      (bind-root #'world/raycaster (raycaster/create grid/grid grid/blocks-vision?))
      (let [width  (tiled/width  tiled-map)
            height (tiled/height tiled-map)]
        (bind-root #'world/explored-tile-corners (atom (g2d/create-grid width height (constantly false))))
        (bind-root #'entities/content-grid (content-grid/create {:cell-size 16  ; FIXME global config
                                                                 :width  width
                                                                 :height height})))
      (bind-root #'world/entity-tick-error nil)
      (bind-root #'entities/ids->eids {})
      (time/init)
      (component/->handle [[:tx/spawn-creatures level]]))))
