(ns forge.screens.world
  (:require [anvil.app :refer [change-screen]]
            [anvil.controls :as controls]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g :refer [set-cursor draw-on-world-view
                                          draw-tiled-map
                                          world-mouse-position world-camera
                                          world-viewport-width world-viewport-height]]
            [anvil.screen :refer [Screen]]
            [anvil.stage :as stage]
            [anvil.system :as system]
            [anvil.world :as world :refer [player-eid explored-tile-corners mouseover-entity mouseover-eid active-entities circle->cells point->entities ray-blocked? line-of-sight? render-z-order]]
            [clojure.gdx.graphics :refer [clear-screen delta-time]]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color :refer [->color]]
            [clojure.gdx.math.shapes :refer [circle->outer-rectangle]]
            [clojure.gdx.scene2d.actor :refer [visible? set-visible] :as actor]
            [clojure.gdx.scene2d.group :refer [children]]
            [clojure.utils :refer [bind-root ->tile sort-by-order pretty-pst]]
            [forge.world :refer [start-world dispose-world]]
            [forge.world.potential-fields :refer [update-potential-fields!
                                                  factions-iterations]]))

(defn- geom-test []
  (let [position (world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (g/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (circle->cells circle))]
      (g/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (g/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam (world-camera)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (g/grid (int left-x) (int bottom-y)
              (inc (int world-viewport-width))
              (+ 2 (int world-viewport-height))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (world/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (g/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (g/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (g/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (->tile (world-mouse-position))
          cell (get world/grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (g/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5]))))))

(defn- debug-render-before-entities []
  (tile-debug))

(defn- debug-render-after-entities []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private explored-tile-color (->color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter* [light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            color/white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

(defn- windows []
  (:windows (stage/get)))

(defn- check-window-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn- close-all-windows []
  (let [windows (children (windows))]
    (when (some visible? windows)
      (run! #(set-visible % false) windows))))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (system/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- time-update []
  (let [delta-ms (min (delta-time) world/max-delta-time)]
    (alter-var-root #'world/elapsed-time + delta-ms)
    (bind-root world/world-delta delta-ms)))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (world/all-entities))]
    (world/remove-entity eid)
    (doseq [component @eid]
      (system/destroy component eid))))

(defn- update-world []
  (system/manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root world/paused? (or world/tick-error
                               (and pausing?
                                    (system/pause-game? (fsm/state-obj @player-eid))
                                    (not (controls/unpaused?)))))
  (when-not world/paused?
    (time-update)
    (let [entities (active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root world/tick-error t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (g/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) color/white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t))))

(defn- render-entities
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              render-z-order)
            system [system/render-below
                    system/render-default
                    system/render-above
                    system/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn- render-world []
  ; FIXME position DRY
  (cam/set-position! (world-camera) (:position @player-eid))
  ; FIXME position DRY
  (draw-tiled-map world/tiled-map
                  (tile-color-setter (cam/position (world-camera))))
  (draw-on-world-view (fn []
                       (debug-render-before-entities)
                       ; FIXME position DRY (from player)
                       (render-entities (map deref (active-entities)))
                       (debug-render-after-entities))))

(deftype WorldScreen []
  Screen
  (enter [_]
    (cam/set-zoom! (world-camera) 0.8))

  (exit [_]
    (set-cursor :cursors/default))

  (render [_]
    (clear-screen color/black)
    (render-world)
    (update-world)
    (controls/world-camera-zoom)
    (check-window-hotkeys)
    (cond (controls/close-windows?)
          (close-all-windows)

          (controls/minimap?)
          (change-screen :screens/minimap)))

  (dispose [_]
    (dispose-world)))

(defn create []
  (stage/create
   {:screen (->WorldScreen)}))
