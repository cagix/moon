(ns cdq.application
  (:require [cdq.application.config :as config]
            [cdq.application.db :as db]
            [cdq.application.ctx-schema :as ctx-schema]
            [cdq.application.potential-fields.update :as potential-fields.update]
            [cdq.application.potential-fields.movement :as potential-fields.movement]
            [cdq.application.raycaster :as raycaster]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.math :as math]
            [cdq.timer :as timer]
            [cdq.tx.spawn-creature]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :as utils :refer [sort-by-order
                                         pretty-pst
                                         safe-merge]]
            [cdq.vector2 :as v]


            [gdl.application]
            [gdl.c :as c]
            [gdl.graphics :as graphics]
            [gdl.math]
            [gdl.tiled :as tiled]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (position [_]
    position)

  (rectangle [_]
    (let [[x y] left-bottom]
      (gdl.math/rectangle x y width height)))

  (overlaps? [this other-entity]
    (gdl.math/overlaps? (entity/rectangle this)
                        (entity/rectangle other-entity)))

  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (entity/position entity)
                             (entity/position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange)))

  (id [{:keys [entity/id]}]
    id)

  (faction [{:keys [entity/faction]}]
    faction)

  (enemy [this]
    (case (entity/faction this)
      :evil :good
      :good :evil))

  (state-k [{:keys [entity/fsm]}]
    (:state fsm))

  (state-obj [this]
    (let [k (entity/state-k this)]
      [k (k this)]))

  (skill-usable-state
    [entity
     {:keys [skill/cooling-down? skill/effects] :as skill}
     effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (not-enough-mana? entity skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable))
  )

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}
                    minimum-size
                    z-orders]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components ctx]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] ctx)))
          {}
          components))

(defn- player-entity-props [start-position {:keys [creature-id
                                                   free-skill-points
                                                   click-distance-tiles]}]
  {:position start-position
   :creature-id creature-id
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [ctx skill]
                                                 (action-bar/add-skill! (c/get-actor ctx :action-bar)
                                                                        skill))
                                 :skill-removed! (fn [ctx skill]
                                                   (action-bar/remove-skill! (c/get-actor ctx :action-bar)
                                                                             skill))
                                 :item-set! (fn [ctx inventory-cell item]
                                              (-> (c/get-actor ctx :windows)
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [ctx inventory-cell]
                                                  (-> (c/get-actor ctx :windows)
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position]
  (cdq.tx.spawn-creature/do! ctx
                             (player-entity-props (utils/tile->middle start-position)
                                                  ctx/player-entity-config)))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- create-actors [ctx]
  [((requiring-resolve 'cdq.ui.dev-menu/create) ctx)
   (action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (c/ui-viewport-width ctx) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(c/ui-viewport-width ctx) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(c/ui-viewport-width ctx)
                                                                       (c/ui-viewport-height ctx)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(extend-type gdl.application.Context
  g/StageActors
  (open-error-window! [ctx throwable]
    (c/add-actor! ctx (error-window/create throwable)))

  (selected-skill [ctx]
    (action-bar/selected-skill (c/get-actor ctx :action-bar))))

(defn- create-game-state [ctx]
  (c/reset-actors! ctx (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((requiring-resolve (g/config ctx :world-fn)) ctx)
        grid (grid/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (cdq.content-grid/create tiled-map (g/config ctx :content-grid-cell-size))
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))

(extend-type gdl.application.Context
  g/Raycaster
  (ray-blocked? [{:keys [ctx/raycaster]} start end]
    (raycaster/blocked? raycaster
                        start
                        end))

  (path-blocked? [{:keys [ctx/raycaster]} start end width]
    (raycaster/path-blocked? raycaster
                             start
                             end
                             width)))

(def ^:private explored-tile-color (graphics/color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

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

(defn- draw-world-map! [{:keys [ctx/tiled-map
                                ctx/raycaster
                                ctx/explored-tile-corners]
                         :as ctx}]
  (c/draw-tiled-map! ctx
                     tiled-map
                     (tile-color-setter raycaster
                                        explored-tile-corners
                                        (c/camera-position ctx))))

(extend-type gdl.application.Context
  g/Time
  (elapsed-time [{:keys [ctx/elapsed-time]}]
    elapsed-time)

  (create-timer [{:keys [ctx/elapsed-time]} duration]
    (timer/create elapsed-time duration))

  (timer-stopped? [{:keys [ctx/elapsed-time]} timer]
    (timer/stopped? elapsed-time timer))

  (reset-timer [{:keys [ctx/elapsed-time]} timer]
    (timer/reset elapsed-time timer))

  (timer-ratio [{:keys [ctx/elapsed-time]} timer]
    (timer/ratio elapsed-time timer)))

(extend-type gdl.application.Context
  cdq.g/Grid
  (grid-cell [{:keys [ctx/grid]} position]
    (grid position))

  (point->entities [{:keys [ctx/grid]} position]
    (grid/point->entities grid position))

  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->cells [{:keys [ctx/grid]} circle]
    (grid/circle->cells grid circle))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(defn- remove-destroyed-entities! [{:keys [ctx/entity-ids] :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (let [id (entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! eid)
    (doseq [component @eid]
      (g/handle-txs! ctx (entity/destroy! component eid ctx))))
  nil)

(extend-type gdl.application.Context
  g/Entities
  (spawn-entity! [{:keys [ctx/id-counter
                          ctx/entity-ids
                          ctx/content-grid
                          ctx/grid]
                   :as ctx}
                  position body components]
    ; TODO SCHEMA COMPONENTS !
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        (create-body ctx/minimum-size ctx/z-orders)
                        (utils/safe-merge (-> components
                                              (assoc :entity/id (swap! id-counter inc))
                                              (create-vs ctx)))))]
      (let [id (entity/id @eid)]
        (assert (number? id))
        (swap! entity-ids assoc id eid))
      (content-grid/add-entity! content-grid eid)
      ; https://github.com/damn/core/issues/58
      ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
      (grid/add-entity! grid eid)
      (doseq [component @eid]
        (g/handle-txs! ctx (entity/create! component eid ctx)))
      eid))

  (move-entity! [{:keys [ctx/content-grid
                         ctx/grid]}
                 eid body direction rotate-in-movement-direction?]
    (content-grid/position-changed! content-grid eid)
    (grid/position-changed! grid eid)
    (swap! eid assoc
           :position (:position body)
           :left-bottom (:left-bottom body))
    (when rotate-in-movement-direction?
      (swap! eid assoc :rotation-angle (v/angle-from-vector direction))))

  (spawn-effect! [ctx position components]
    (g/spawn-entity! ctx
                     position
                     (g/config ctx :effect-body-props)
                     components)))

(extend-type gdl.application.Context
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type gdl.application.Context
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (filter #(= "properties" (namespace %)) (keys (:schemas db))))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [{:keys [ctx/db] :as ctx}
                     property]
    (let [new-db (db/update db property)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db)))

  (delete-property! [{:keys [ctx/db] :as ctx}
                     property-id]
    (let [new-db (db/delete db property-id)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db))))

(defn- geom-test* [ctx]
  (let [position (c/world-mouse-position ctx)
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
  (c/handle-draws! ctx (geom-test* ctx)))

(defn- highlight-mouseover-tile* [ctx]
  (let [[x y] (mapv int (c/world-mouse-position ctx))
        cell (g/grid-cell ctx [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [ctx]
  (c/handle-draws! ctx (highlight-mouseover-tile* ctx)))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- draw-tile-grid* [ctx]
  (when ctx/show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (c/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (c/world-viewport-width ctx)))
        (+ 2 (int (c/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-tile-grid [ctx]
  (c/handle-draws! ctx (draw-tile-grid* ctx)))

(defn- draw-cell-debug* [ctx]
  (apply concat
         (for [[x y] (c/visible-tiles ctx)
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
  (c/handle-draws! ctx (draw-cell-debug* ctx)))

(defn- render-entities! [{:keys [ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
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
         (c/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (c/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (c/handle-draws! ctx (draw-body-rect entity :red))
         (pretty-pst t))))))

(defn- camera-controls! [ctx]
  (let [controls (g/config ctx :controls)
        zoom-speed (g/config ctx :zoom-speed)]
    (when (c/key-pressed? ctx (:zoom-in controls))  (c/inc-zoom! ctx    zoom-speed))
    (when (c/key-pressed? ctx (:zoom-out controls)) (c/inc-zoom! ctx (- zoom-speed)))))

(defn- get-active-entities [{:keys [ctx/content-grid
                                    ctx/player-eid]}]
  (content-grid/active-entities content-grid @player-eid))

(defn- player-state-handle-click! [{:keys [ctx/player-eid] :as ctx}]
  (g/handle-txs! ctx
                 (state/manual-tick (entity/state-obj @player-eid)
                                    player-eid
                                    ctx))
  nil)

(defn- update-mouseover-entity! [{:keys [ctx/player-eid
                                         ctx/mouseover-eid]
                                  :as ctx}]
  (let [new-eid (if (c/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (g/point->entities ctx (c/world-mouse-position ctx)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(g/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(defn- pause-game? [{:keys [ctx/player-eid] :as ctx}]
  (let [controls (g/config ctx :controls)]
    (or #_error
        (and (g/config ctx :pausing?)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (c/key-just-pressed? ctx (:unpause-once controls))
                      (c/key-pressed? ctx (:unpause-continously controls))))))))

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
        (throw (ex-info "entity/id: " (entity/id @eid) t)))))
   (catch Throwable t
     (pretty-pst t)
     (g/open-error-window! ctx t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- assoc-delta-time
  [ctx]
  (assoc ctx :ctx/delta-time (min (c/delta-time ctx) ctx/max-delta)))

(defn- update-elapsed-time
  [{:keys [ctx/delta-time]
    :as ctx}]
  (update ctx :ctx/elapsed-time + delta-time))

(defn- render-game-state! [{:keys [ctx/player-eid] :as ctx}]
  (ctx-schema/validate ctx)
  (let [ctx (assoc ctx :ctx/active-entities (get-active-entities ctx))]
    (c/set-camera-position! ctx (entity/position @player-eid))
    (c/clear-screen! ctx)
    (draw-world-map! ctx)
    (c/draw-on-world-viewport! ctx [draw-tile-grid
                                    draw-cell-debug
                                    render-entities!
                                    ;geom-test
                                    highlight-mouseover-tile])
    (c/draw-stage! ctx)
    (c/update-stage! ctx)
    (player-state-handle-click! ctx)
    (let [ctx (update-mouseover-entity! ctx)
          ctx (assoc-paused ctx)
          ctx (if (:ctx/paused? ctx)
                ctx
                (let [ctx (-> ctx
                              assoc-delta-time
                              update-elapsed-time)]
                  (potential-fields.update/do! ctx)
                  (tick-entities! ctx)
                  ctx))]
      (remove-destroyed-entities! ctx) ; do not pause as pickup item should be destroyed
      (camera-controls! ctx)
      ctx)))

(defn reset-game-state! []
  (swap! gdl.application/state create-game-state))

(defn -main []
  (let [config (config/create "config.edn")]
    (run! require (:requires config))
    (gdl.application/start! config
                            (fn [context]
                              (-> context
                                  (safe-merge {:ctx/config config
                                               :ctx/db (db/create (:db config))})
                                  create-game-state))
                            render-game-state!

                              #_(dispose! [_]
                                ; nil
                                ; TODO dispose world tiled-map/level resources?
                                )
                              )))
