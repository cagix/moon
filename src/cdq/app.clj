(ns cdq.app
  (:require [clojure.utils :refer [dispose safe-merge tile->middle]]
            [data.grid2d :as g2d]
            [gdl.app :as app]
            [gdl.context :as gdl.context]
            [gdl.ui :as ui]
            [gdl.graphics]
            [gdl.graphics.camera :as cam]
            [gdl.ui :as ui]
            [gdl.tiled :as tiled]
            [cdq.grid :as grid]
            [cdq.level :refer [generate-level]]
            [cdq.context :refer [spawn-creature]]
            [cdq.context.stage-actors :as stage-actors]
            [cdq.context.content-grid :as content-grid]
            cdq.graphics
            cdq.graphics.tiled-map)
  (:gen-class))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- ->grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- create-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (->grid-cell position
                        (case (tiled/movement-property tiled-map position)
                          "none" :none
                          "air"  :air
                          "all"  :all))))))

; TODO this passing w. world props ...
; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- spawn-player-entity [context start-position]
  (spawn-creature context (player-entity-props start-position)))

(defn- spawn-enemies! [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle))))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster [grid]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell grid/blocks-vision?))
    [arr width height]))

(defn- explored-tile-corners [tiled-map]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn- create [context config]
  (let [context (safe-merge context
                            {
                             ;; - before here - application context - does not change on level/game restart -
                             :gdl.context/elapsed-time 0
                             :cdq.context/player-message (atom (:player-message config))})]
    (gdl.context/reset-stage context
                             (stage-actors/create context)) ; TODO this is not part of context keys anymore, can't be dispatched ... on actors
    ; so it should be part of stage/ui then? for resett-ing later ?
    (let [level (generate-level context
                                (gdl.context/build context (:world-id config)))
          tiled-map (:tiled-map level)
          grid (create-grid tiled-map)
          context (safe-merge context
                              {:cdq.context/error nil
                               :cdq.context/level level
                               :cdq.context/tiled-map tiled-map
                               :cdq.context/grid grid
                               :cdq.context/explored-tile-corners (explored-tile-corners tiled-map)
                               :cdq.context/content-grid (content-grid/create tiled-map (:content-grid config))
                               :cdq.context/entity-ids (atom {})
                               :cdq.context/raycaster (create-raycaster grid)
                               :cdq.context/factions-iterations (:factions-iterations config)})
          context (assoc context :cdq.context/player-eid (spawn-player-entity context (:start-position level)))]
      (spawn-enemies! context tiled-map)
      context)))

(defn- dispose! [context]
  ;;
  ; TODO dispose :gdl.context/sd-texture
  (dispose (:gdl.context/assets context))
  (dispose (:gdl.context/batch  context))
  (run! dispose (vals (:gdl.context/cursors context)))
  (dispose (:gdl.context/default-font context))
  (ui/dispose!)
  (dispose (:gdl.context/stage context))
  ;;

  (dispose (:cdq.context/tiled-map context))  ; TODO ! this also if world restarts !!
  )

(defn- set-camera-on-player! [{:keys [gdl.context/world-viewport
                                      cdq.context/player-eid]
                               :as context}]
  (cam/set-position! (:camera world-viewport)
                     (:position @player-eid))
  context)

(defn- render [context]
  (set-camera-on-player! context)
  (reduce (fn [context f]
            (f context))
          context
          [gdl.graphics/clear-screen
           cdq.graphics.tiled-map/render
           cdq.graphics/draw-world-view
           gdl.graphics/draw-stage

           ; updates
           gdl.context/update-stage
           cdq.context/handle-player-input
           cdq.context/update-mouseover-entity
           cdq.context/update-paused-state
           cdq.context/progress-time-if-not-paused
           cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
           gdl.context/check-camera-controls
           cdq.context/check-ui-key-listeners]))


(defn -main []
  (app/start "app.edn"
             create
             dispose!
             render))
