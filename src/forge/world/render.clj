(ns forge.world.render
  (:require [anvil.app :refer [world-viewport-width world-viewport-height]]
            [anvil.entity :as entity :refer [line-of-sight?]]
            [anvil.graphics :as g]
            [anvil.grid :as grid]
            [anvil.raycaster :refer [ray-blocked?]]
            [anvil.level :as level :refer [explored-tile-corners]]
            [clojure.component :as component]
            [clojure.gdx.graphics.camera :as cam]
            [clojure.gdx.graphics.color :as color :refer [->color]]
            [clojure.gdx.math.shapes :refer [circle->outer-rectangle]]
            [clojure.utils :refer [sort-by-order pretty-pst]]
            [forge.world.potential-fields :refer [factions-iterations]]))

(defn- geom-test []
  (let [position (g/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (g/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells circle))]
      (g/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (g/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug []
  (let [cam (g/world-camera)
        [left-x right-x bottom-y top-y] (cam/frustum cam)]

    (when tile-grid?
      (g/grid (int left-x) (int bottom-y)
              (inc (int world-viewport-width))
              (+ 2 (int world-viewport-height))
              1 1 [1 1 1 0.8]))

    (doseq [[x y] (cam/visible-tiles cam)
            :let [cell (grid/get [x y])]
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
    (let [[x y] (mapv int (g/world-mouse-position))
          cell (grid/get [x y])]
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
              (swap! explored-tile-corners assoc (mapv int position) true))
            color/white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

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
  (let [player @entity/player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              entity/render-z-order)
            system [component/render-below
                    component/render-default
                    component/render-above
                    component/render-info]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

(defn render-world []
  ; FIXME position DRY
  (cam/set-position! (g/world-camera)
                     (:position @entity/player-eid))
  ; FIXME position DRY
  (g/draw-tiled-map level/tiled-map
                    (tile-color-setter (cam/position (g/world-camera))))
  (g/draw-on-world-view (fn []
                          (debug-render-before-entities)
                          ; FIXME position DRY (from player)
                          (render-entities (map deref (entity/active-entities)))
                          (debug-render-after-entities))))
