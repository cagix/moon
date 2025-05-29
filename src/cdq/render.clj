(ns cdq.render
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.state :as state]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.stacktrace :as stacktrace]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.math :as math]
            [gdl.graphics.color :as color]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.utils :as utils]))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- render-entities! [{:keys [ctx/graphics
                                 ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (utils/sort-by-order (group-by :z-order entities)
                                                    first
                                                    ctx/render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (graphics/handle-draws! graphics (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (graphics/handle-draws! graphics (render! component entity ctx)))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect entity :red))
         (stacktrace/pretty-pst t))))))

(defn- remove-destroyed-entities! [{:keys [ctx/entity-ids] :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (g/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/destroy! component eid ctx))))
  nil)

(defn- camera-controls! [{:keys [ctx/config
                                 ctx/graphics
                                 ctx/input]}]
  (let [controls (:controls config)
        zoom-speed (:zoom-speed config)]
    (when (input/key-pressed? input (:zoom-in controls))  (graphics/inc-zoom! graphics    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (graphics/inc-zoom! graphics (- zoom-speed)))))

(defn- pause-game? [{:keys [ctx/config
                            ctx/input
                            ctx/player-eid]}]
  (let [controls (:controls config)]
    (or #_error
        (and (:pausing? config)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (input/key-just-pressed? input (:unpause-once controls))
                      (input/key-pressed? input (:unpause-continously controls))))))))

(defn- assoc-paused [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))

(defn- tick-entities!
  [{:keys [ctx/active-entities] :as ctx}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (g/handle-txs! ctx (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info (str "entity/id: " (entity/id @eid)) {} t)))))
   (catch Throwable t
     (stacktrace/pretty-pst t)
     (g/open-error-window! ctx t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- assoc-delta-time
  [{:keys [ctx/graphics]
    :as ctx}]
  (assoc ctx :ctx/delta-time (min (graphics/delta-time graphics) ctx/max-delta)))

(defn- update-elapsed-time
  [{:keys [ctx/delta-time]
    :as ctx}]
  (update ctx :ctx/elapsed-time + delta-time))

(defn- update-potential-fields!
  [{:keys [ctx/potential-field-cache
           ctx/grid
           ctx/active-entities]}]
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))


(defn- update-mouseover-entity! [{:keys [ctx/player-eid
                                         ctx/mouseover-eid
                                         ctx/grid]
                                  :as ctx}]
  (let [new-eid (if (g/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities grid (g/world-mouse-position ctx)))]
                    (->> ctx/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(g/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(defn- player-state-handle-click! [{:keys [ctx/player-eid] :as ctx}]
  (g/handle-txs! ctx
                 (state/manual-tick (entity/state-obj @player-eid)
                                    player-eid
                                    ctx))
  nil)

(defn- highlight-mouseover-tile* [{:keys [ctx/grid] :as ctx}]
  (let [[x y] (mapv int (g/world-mouse-position ctx))
        cell (grid/cell grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [{:keys [ctx/graphics]
                                  :as ctx}]
  (graphics/handle-draws! graphics (highlight-mouseover-tile* ctx)))

(defn- geom-test* [{:keys [ctx/grid] :as ctx}]
  (let [position (g/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [{:keys [ctx/graphics]
                   :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- draw-tile-grid* [graphics]
  (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum graphics)]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (graphics/world-viewport-width graphics)))
      (+ 2 (int (graphics/world-viewport-height graphics)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics]}]
  (when ctx/show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* graphics))))

(defn- draw-cell-debug* [{:keys [ctx/graphics
                                 ctx/grid]}]
  (apply concat
         (for [[x y] (graphics/visible-tiles graphics)
               :let [cell (grid/cell grid [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and ctx/show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction ctx/show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (ctx/factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- draw-cell-debug [{:keys [ctx/graphics]
                         :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

; Now I get it - we do not need to depend on concretions here -> that's why its so complicated
; also its side effects ... ?? -> transactions ?!

; Wait ! we make the protocols inside cdq.render -> get-active-entities, clear-screen, etc. and then the details defined over that..
; maybe even @ update pf etc can be made simpler?

(defn- draw-world-map! [{:keys [ctx/graphics
                                ctx/tiled-map
                                ctx/raycaster
                                ctx/explored-tile-corners]}]
  (graphics/draw-tiled-map! graphics
                            tiled-map
                            (tile-color-setter/create
                             {:raycaster raycaster
                              :explored-tile-corners explored-tile-corners
                              :light-position (graphics/camera-position graphics)
                              :explored-tile-color (color/create 0.5 0.5 0.5 1)
                              :see-all-tiles? false})))

(defn do! [{:keys [ctx/graphics
                   ctx/player-eid
                   ctx/stage]
            :as ctx}]
  (let [ctx (assoc ctx :ctx/active-entities (g/get-active-entities ctx))]
    (graphics/set-camera-position! graphics (entity/position @player-eid))
    (graphics/clear-screen! graphics)
    (draw-world-map! ctx)
    (graphics/draw-on-world-viewport! graphics
                                      (fn []
                                        (doseq [f [draw-tile-grid
                                                   draw-cell-debug
                                                   render-entities!
                                                   ;geom-test
                                                   highlight-mouseover-tile]]
                                          (f ctx))))
    (ui/act! stage ctx)
    (ui/draw! stage ctx)
    (player-state-handle-click! ctx)
    (let [ctx (update-mouseover-entity! ctx)
          ctx (assoc-paused ctx)
          ctx (if (:ctx/paused? ctx)
                ctx
                (let [ctx (-> ctx
                              assoc-delta-time
                              update-elapsed-time)]
                  (update-potential-fields! ctx)
                  (tick-entities! ctx)
                  ctx))]
      (remove-destroyed-entities! ctx) ; do not pause as pickup item should be destroyed
      (camera-controls! ctx)
      ctx)))
