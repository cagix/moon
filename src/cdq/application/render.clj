(ns cdq.application.render
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.potential-field :as potential-field]
            [cdq.math :as math]
            [cdq.ui.error-window :as error-window]
            [cdq.utils :refer [bind-root
                               sort-by-order
                               pretty-pst
                               handle-txs!]]
            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]))

(defn- geom-test! []
  (let [position (viewport/mouse-position ctx/world-viewport)
        radius 0.8
        circle {:position position :radius radius}]
    (draw/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells ctx/grid circle))]
      (draw/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (math/circle->outer-rectangle circle)]
      (draw/rectangle x y width height [0 0 1 1]))))

(def ^:private explored-tile-color (graphics/color 0.5 0.5 0.5 1))

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

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color graphics/black)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? graphics/white base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              graphics/white))))))

(defn- active-entities []
  (content-grid/active-entities ctx/content-grid @ctx/player-eid))

(defn- draw-tiled-map! []
  (tiled/draw! (ctx/get-tiled-map-renderer ctx/tiled-map)
               ctx/tiled-map
               (tile-color-setter ctx/raycaster
                                  ctx/explored-tile-corners
                                  (camera/position (:camera ctx/world-viewport)))
               (:camera ctx/world-viewport)))

(defn- draw-on-world-view! [draw-fns]
  (batch/draw-on-viewport! ctx/batch
                           ctx/world-viewport
                           (fn []
                             (draw/with-line-width ctx/world-unit-scale
                               (fn []
                                 (reset! ctx/unit-scale ctx/world-unit-scale)
                                 (doseq [f draw-fns]
                                   (f))
                                 (reset! ctx/unit-scale 1))))))

(defn- debug-draw-before-entities! []
  (let [cam (:camera ctx/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when ctx/show-tile-grid?
      (draw/grid (int left-x) (int bottom-y)
                 (inc (int (:width  ctx/world-viewport)))
                 (+ 2 (int (:height ctx/world-viewport)))
                 1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (ctx/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and ctx/show-cell-entities? (seq (:entities cell*)))
        (draw/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
        (draw/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when-let [faction ctx/show-potential-field-colors?]
        (let [{:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (ctx/factions-iterations faction))]
              (draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (draw/rectangle x y (:width entity) (:height entity) color)))

(defn- render-entities! []
  (let [entities (map deref ctx/active-entities)
        player @ctx/player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              ctx/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (entity/line-of-sight? player entity))]
      (try
       (when ctx/show-body-bounds?
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- highlight-mouseover-tile! []
  (let [[x y] (mapv int (viewport/mouse-position ctx/world-viewport))
        cell (ctx/grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      (draw/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5])))))

(defn- player-state-handle-click []
  (-> @ctx/player-eid
      entity/state-obj
      state/manual-tick
      handle-txs!))

(defn- update-mouseover-entity! []
  (let [new-eid (if (ui/hit ctx/stage (viewport/mouse-position ctx/ui-viewport))
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities ctx/grid
                                                           (viewport/mouse-position ctx/world-viewport)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(entity/line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'ctx/mouseover-eid new-eid)))

(defn- pause-game? []
  (or #_error
      (and ctx/pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (input/key-just-pressed? (get ctx/controls :unpause-once))
                    (input/key-pressed? (get ctx/controls :unpause-continously)))))))

(defn- update-time! []
  (let [delta-ms (min (graphics/delta-time) ctx/max-delta)]
    (alter-var-root #'ctx/elapsed-time + delta-ms)
    (bind-root #'ctx/delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-field/tick! ctx/potential-field-cache
                           ctx/grid
                           faction
                           ctx/active-entities
                           max-iterations)))

(defn- tick-entities! []
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid ctx/active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (handle-txs! (entity/tick! [k v] eid)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (ui/add! ctx/stage (error-window/create t))
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- remove-destroyed-entities! []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @ctx/entity-ids))]
    (let [id (:entity/id @eid)]
      (assert (contains? @ctx/entity-ids id))
      (swap! ctx/entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! eid)
    (doseq [component @eid]
      (handle-txs! (entity/destroy! component eid)))))

(defn- camera-controls! []
  (when (input/key-pressed? (get ctx/controls :zoom-in))
    (camera/inc-zoom! (:camera ctx/world-viewport) ctx/zoom-speed))
  (when (input/key-pressed? (get ctx/controls :zoom-out))
    (camera/inc-zoom! (:camera ctx/world-viewport) (- ctx/zoom-speed))))

(defn do! []
  (bind-root #'ctx/active-entities (active-entities))
  (camera/set-position! (:camera ctx/world-viewport)
                        (:position @ctx/player-eid))
  (graphics/clear-screen!)
  (draw-tiled-map!)
  (draw-on-world-view! [debug-draw-before-entities!
                        render-entities!
                        ; geom-test!
                        highlight-mouseover-tile!])
  (ui/draw! ctx/stage)
  (ui/act! ctx/stage)
  (player-state-handle-click)
  (update-mouseover-entity!)
  (bind-root #'ctx/paused? (pause-game?))
  (when-not ctx/paused?
    (update-time!)
    (update-potential-fields!)
    (tick-entities!))
  (remove-destroyed-entities!) ; do not pause as pickup item should be destroyed
  (camera-controls!))
