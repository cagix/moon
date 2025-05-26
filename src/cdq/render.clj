(ns cdq.render
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.g :as g]
            [cdq.stacktrace :as stacktrace]
            [cdq.math :as math]
            [gdl.utils :as utils]))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- render-entities! [{:keys [ctx/active-entities
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
         (g/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (g/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (g/handle-draws! ctx (draw-body-rect entity :red))
         (stacktrace/pretty-pst t))))))

(defn- remove-destroyed-entities! [{:keys [ctx/entity-ids] :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (g/context-entity-remove! ctx eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/destroy! component eid ctx))))
  nil)

(defn- camera-controls! [ctx]
  (let [controls (g/config ctx :controls)
        zoom-speed (g/config ctx :zoom-speed)]
    (when (g/key-pressed? ctx (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (g/key-pressed? ctx (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed)))))

(defn- pause-game? [{:keys [ctx/player-eid] :as ctx}]
  (let [controls (g/config ctx :controls)]
    (or #_error
        (and (g/config ctx :pausing?)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (g/key-just-pressed? ctx (:unpause-once controls))
                      (g/key-pressed? ctx (:unpause-continously controls))))))))

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
  [ctx]
  (assoc ctx :ctx/delta-time (min (g/delta-time ctx) ctx/max-delta)))

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
                                         ctx/mouseover-eid]
                                  :as ctx}]
  (let [new-eid (if (g/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (g/point->entities ctx (g/world-mouse-position ctx)))]
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

(defn- highlight-mouseover-tile* [ctx]
  (let [[x y] (mapv int (g/world-mouse-position ctx))
        cell (g/grid-cell ctx [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [ctx]
  (g/handle-draws! ctx (highlight-mouseover-tile* ctx)))

(defn- geom-test* [ctx]
  (let [position (g/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (g/circle->cells ctx circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [ctx]
  (g/handle-draws! ctx (geom-test* ctx)))

(defn- draw-tile-grid* [ctx]
  (when ctx/show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (g/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (g/world-viewport-width ctx)))
        (+ 2 (int (g/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-tile-grid [ctx]
  (g/handle-draws! ctx (draw-tile-grid* ctx)))

(defn- draw-cell-debug* [ctx]
  (apply concat
         (for [[x y] (g/visible-tiles ctx)
               :let [cell (g/grid-cell ctx [x y])]
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

(defn- draw-cell-debug [ctx]
  (g/handle-draws! ctx (draw-cell-debug* ctx)))

; Now I get it - we do not need to depend on concretions here -> that's why its so complicated
; also its side effects ... ?? -> transactions ?!

; Wait ! we make the protocols inside cdq.render -> get-active-entities, clear-screen, etc. and then the details defined over that..
; maybe even @ update pf etc can be made simpler?

(defn do! [{:keys [ctx/player-eid] :as ctx}]
  (let [ctx (assoc ctx :ctx/active-entities (g/get-active-entities ctx))]
    (g/set-camera-position! ctx (entity/position @player-eid))
    (g/clear-screen! ctx)
    (g/draw-world-map! ctx)
    (g/draw-on-world-viewport! ctx [draw-tile-grid
                                    draw-cell-debug
                                    render-entities!
                                    ;geom-test
                                    highlight-mouseover-tile])
    (g/draw-stage! ctx)
    (g/update-stage! ctx)
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
