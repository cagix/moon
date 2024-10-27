(ns moon.world
  (:require [data.grid2d :as g2d]
            [gdl.graphics :as gdx.graphics]
            [gdl.graphics.camera :as cam]
            [gdl.graphics.color :as color]
            [gdl.math.shape :as shape]
            [gdl.tiled :as t]
            [gdl.utils :refer [dispose ->tile tile->middle]]
            [moon.component :refer [defc] :as component]
            [moon.db :as db]
            [moon.entity :as entity]
            [moon.graphics :as g]
            [moon.level :as level]
            [moon.screen :as screen]
            [moon.stage :as stage]
            [moon.world.content-grid :as content-grid]
            [moon.world.raycaster :as raycaster]))

(declare paused?
         ^{:doc "The game logic update delta-time. Different then gdx.graphics/delta-time because it is bounded by a maximum value for entity movement speed."}
         delta-time
         ^{:doc "The elapsed in-game-time (not counting when game is paused)."}
         elapsed-time
         ^{:doc "The game-logic frame number, starting with 1. (not counting when game is paused)"}
         logic-frame)

(defn timer [duration]
  {:pre [(>= duration 0)]}
  {:duration duration
   :stop-time (+ elapsed-time duration)})

(defn stopped? [{:keys [stop-time]}]
  (>= elapsed-time stop-time))

(defn reset [{:keys [duration] :as counter}]
  (assoc counter :stop-time (+ elapsed-time duration)))

(defn finished-ratio [{:keys [duration stop-time] :as counter}]
  {:post [(<= 0 % 1)]}
  (if (stopped? counter)
    0
    ; min 1 because floating point math inaccuracies
    (min 1 (/ (- stop-time elapsed-time) duration))))

(declare grid)

(load "world/grid")

(declare ^:private raycaster)

(defn ray-blocked? [start target]
  (raycaster/blocked? raycaster start target))

(defn path-blocked?
  "path-w in tiles. casts two rays."
  [start target path-w]
  (raycaster/path-blocked? raycaster start target path-w))

(declare ^:private content-grid
         player)

(defn active-entities []
  (content-grid/active-entities content-grid @player))

(declare ^:private ids->eids)

(defn all-entities [] (vals ids->eids))
(defn get-entity [id] (get ids->eids id))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (cam/position (g/world-camera))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (g/world-viewport-width))  2)))
     (<= ydist (inc (/ (float (g/world-viewport-height)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

; does not take into account size of entity ...
; => assert bodies <1 width then
(defn line-of-sight? [source target]
  (and (or (not (:entity/player? source))
           (on-screen? target))
       (not (and los-checks?
                 (ray-blocked? (:position source) (:position target))))))

(def mouseover-eid nil)

(defn mouseover-entity []
  (when-let [eid mouseover-eid]
    @eid))

(declare tiled-map)

(defn clear-tiled-map []
  (when (bound? #'tiled-map)
    (dispose tiled-map)))

(declare explored-tile-corners)

(load "world/render")

(declare entity-tick-error)

(defn- init! [tiled-map]
  (clear-tiled-map)
  (bind-root #'tiled-map tiled-map)
  (bind-root #'grid (create-grid tiled-map))
  (bind-root #'raycaster (raycaster/create grid blocks-vision?))
  (let [width  (t/width  tiled-map)
        height (t/height tiled-map)]
    (bind-root #'explored-tile-corners (atom (g2d/create-grid width height (constantly false))))
    (bind-root #'content-grid (content-grid/create {:cell-size 16  ; FIXME global config
                                                    :width  width
                                                    :height height})))
  (bind-root #'entity-tick-error nil)
  (bind-root #'elapsed-time 0)
  (bind-root #'logic-frame 0)
  (bind-root #'ids->eids {}))

(def ^:private ^:dbg-flag spawn-enemies? true)

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- spawn-entities [{:keys [tiled-map start-position]}]
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

(defn start [world-id]
  (screen/change :screens/world)
  (stage/reset (component/create [:world/widgets]))
  (let [level (level/generate world-id)]
    (init! (:tiled-map level))
    (spawn-entities level)))

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
