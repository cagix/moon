(ns moon.world
  (:require [data.grid2d :as g2d]
            [clj-commons.pretty.repl :refer [pretty-pst]]
            [gdl.graphics :as gdx.graphics]
            [gdl.graphics.camera :as cam]
            [gdl.graphics.color :as color]
            [gdl.math.shape :as shape]
            [gdl.tiled :as t]
            [gdl.utils :refer [dispose ->tile tile->middle sort-by-order]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.level :as level]
            [moon.stage :as stage]
            [moon.world.content-grid :as content-grid]
            [moon.world.raycaster :as raycaster])
  (:load "world/grid"
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
      "world/render")

(declare entity-tick-error)

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

(defn get-window [k]
  (get (:windows (stage/get)) k))
