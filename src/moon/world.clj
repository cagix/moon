(ns moon.world
  (:require [gdl.graphics :as gdx.graphics]
            [gdl.graphics.camera :as cam]
            [gdl.graphics.color :as color]
            [gdl.input :refer [key-pressed? key-just-pressed?]]
            [gdl.utils :refer [dispose ->tile tile->middle safe-merge sort-by-order]]
            [clj-commons.pretty.repl :refer [pretty-pst]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [data.grid2d :as g2d]
            [moon.graphics :as g]
            [moon.ui.error-window :refer [error-window!]]
            [moon.stage :as stage]
            [gdl.math.shape :as shape]
            [gdl.tiled :as t]
            [moon.entity :as entity]
            [moon.level :as level]
            [moon.world.content-grid :as content-grid]
            [moon.world.raycaster :as raycaster])
  (:load "world/grid"
         "world/potential_fields"
         "world/time"))

(defn- init-raycaster []
  (def ^:private raycaster (raycaster/create grid blocks-vision?)))

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

(declare ^:private content-grid
         player)

(defn- init-content-grid! [opts]
  (.bindRoot #'content-grid (content-grid/create opts)))

(defn active-entities []
  (content-grid/active-entities content-grid @player))

(load "world/entities"
      "world/mouseover_entity"
      "world/debug_render"
      "world/render")

(declare ^:private entity-tick-error)

(defn init! [tiled-map]
  (init-tiled-map tiled-map)
  (.bindRoot #'entity-tick-error nil)
  (init-time!)
  (let [w (t/width  tiled-map)
        h (t/height tiled-map)]
    (init-grid! w h (fn [position]
                      (case (level/movement-property tiled-map position)
                        "none" :none
                        "air"  :air
                        "all"  :all)))
    (init-raycaster)
    (init-content-grid! {:cell-size 16 :width w :height h})
    (init-explored-tile-corners w h))
  (init-ids->eids))

(declare start)

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn spawn-entities [{:keys [tiled-map start-position]}]
  (component/->handle
   (for [creature (cons {:position start-position
                         :creature-id :creatures/vampire
                         :components {:entity/fsm {:fsm :fsms/player
                                                   :initial-state :player-idle}
                                      :entity/faction :good
                                      :entity/player? true
                                      :entity/free-skill-points 3
                                      :entity/clickable {:type :clickable/player}
                                      :entity/click-distance-tiles 1.5}}
                        (when spawn-enemies?
                          (for [[position creature-id] (t/positions-with-property tiled-map :creatures :id)]
                            {:position position
                             :creature-id (keyword creature-id)
                             :components {:entity/fsm {:fsm :fsms/npc
                                                       :initial-state :npc-sleeping}
                                          :entity/faction :evil}})))]
     [:tx/creature (update creature :position tile->middle)])))

(defc :tx/add-to-world
  (component/handle [[_ eid]]
    (let [id (:entity/id @eid)]
      (assert (number? id))
      (alter-var-root #'ids->eids assoc id eid))
    (content-grid/update-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid-add-entity! eid)
    nil))

(defc :tx/remove-from-world
  (component/handle [[_ eid]]
    (let [id (:entity/id @eid)]
      (assert (contains? ids->eids id))
      (alter-var-root #'ids->eids dissoc id))
    (content-grid/remove-entity! eid)
    (grid-remove-entity! eid)
    nil))

(defc :tx/position-changed
  (component/handle [[_ eid]]
    (content-grid/update-entity! content-grid eid)
    (grid-entity-position-changed! eid)
   nil))

(def ^:private ^:dbg-flag pausing? true)

(defn- player-state-pause-game? [] (entity/pause-game? (entity/state-obj @player)))
(defn- player-update-state      [] (entity/manual-tick (entity/state-obj @player)))

(defn- player-unpaused? []
  (or (key-just-pressed? :keys/p)
      (key-pressed? :keys/space))) ; FIXMe :keys? shouldnt it be just :space?

(defn- update-game-paused []
  (.bindRoot #'paused? (or entity-tick-error
                                 (and pausing?
                                      (player-state-pause-game?)
                                      (not (player-unpaused?)))))
  nil)

(defn tick! []
  (cam/set-position! (g/world-camera) (:position @player))
  (render-tiled-map! (cam/position (g/world-camera)))
  (g/render-world-view! (fn []
                          (render-before-entities)
                          (render-entities! (map deref (active-entities)))
                          (render-after-entities)))
  (component/->handle
   [player-update-state
    ; this do always so can get debug info even when game not running
    update-mouseover-entity!
    update-game-paused
    #(when-not paused?
       (update-time! (min (gdx.graphics/delta-time) entity/max-delta-time))
       (let [entities (active-entities)]
         (update-potential-fields! entities)
         (try (run! tick-system entities)
              (catch Throwable t
                (error-window! t)
                (.bindRoot #'entity-tick-error t))))
       nil)
    ; do not pause this as for example pickup item, should be destroyed.
    remove-destroyed-entities!]))

(defn get-window [k]
  (get (:windows (stage/get)) k))
