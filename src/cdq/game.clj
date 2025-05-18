(ns cdq.game
  (:require [cdq.ctx :as ctx]
            [cdq.content-grid :as content-grid]
            [cdq.db :as db]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.potential-field :as potential-field]
            [cdq.math :as math]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.dev-menu :as dev-menu]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [cdq.ui.windows]
            [cdq.utils :refer [bind-root
                               io-slurp-edn
                               create-config
                               sort-by-order
                               pretty-pst
                               handle-txs!
                               tile->middle
                               safe-get
                               mapvals]]
            [gdl.assets :as assets]
            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :refer [dispose!]]))

(defn- geom-test! []
  (let [position (viewport/mouse-position ctx/world-viewport)
        radius 0.8
        circle {:position position :radius radius}]
    (draw/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells ctx/grid circle))]
      (draw/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (math/circle->outer-rectangle circle)]
      (draw/rectangle x y width height [0 0 1 1]))))

(defn- highlight-mouseover-tile! []
  (let [[x y] (mapv int (viewport/mouse-position ctx/world-viewport))
        cell (ctx/grid [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      (draw/rectangle x y 1 1
                      (case (:movement @cell)
                        :air  [1 1 0 0.5]
                        :none [1 0 0 0.5])))))

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

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

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

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] ctx/factions-iterations]
    (potential-field/tick! ctx/potential-field-cache
                           ctx/grid
                           faction
                           ctx/active-entities
                           max-iterations)))

(defn- update-time! []
  (let [delta-ms (min (graphics/delta-time) ctx/max-delta)]
    (alter-var-root #'ctx/elapsed-time + delta-ms)
    (bind-root #'ctx/delta-time delta-ms)))

(defn- camera-controls! []
  (when (input/key-pressed? (get ctx/controls :zoom-in))
    (camera/inc-zoom! (:camera ctx/world-viewport) ctx/zoom-speed))
  (when (input/key-pressed? (get ctx/controls :zoom-out))
    (camera/inc-zoom! (:camera ctx/world-viewport) (- ctx/zoom-speed))))

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

(defn- pause-game? []
  (or #_error
      (and ctx/pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (input/key-just-pressed? (get ctx/controls :unpause-once))
                    (input/key-pressed? (get ctx/controls :unpause-continously)))))))

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

(defn- player-state-handle-click []
  (-> @ctx/player-eid
      entity/state-obj
      state/manual-tick
      handle-txs!))

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

(defn- draw-tiled-map! []
  (tiled/draw! (ctx/get-tiled-map-renderer ctx/tiled-map)
               ctx/tiled-map
               (tile-color-setter ctx/raycaster
                                  ctx/explored-tile-corners
                                  (camera/position (:camera ctx/world-viewport)))
               (:camera ctx/world-viewport)))

(defn- active-entities []
  (content-grid/active-entities ctx/content-grid @ctx/player-eid))

(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id (:creature-id ctx/player-entity-config)
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [skill]
                                                 (-> ctx/stage
                                                     :action-bar
                                                     (action-bar/add-skill! skill)))
                                 :skill-removed! (fn [skill]
                                                   (-> ctx/stage
                                                       :action-bar
                                                       (action-bar/remove-skill! skill)))
                                 :item-set! (fn [inventory-cell item]
                                              (-> ctx/stage
                                                  :windows
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [inventory-cell]
                                                  (-> ctx/stage
                                                      :windows
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points (:free-skill-points ctx/player-entity-config)
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles (:click-distance-tiles ctx/player-entity-config)}})

(defn- spawn-player [start-position]
  [[:tx/spawn-creature (player-entity-props start-position)]])

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position tile->middle)]))

(declare reset-game!)

(defn- reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage (ui/stage (:java-object ctx/ui-viewport)
                                   (:java-object ctx/batch)
                                   [(dev-menu/create #'reset-game!)
                                    (action-bar/create :id :action-bar)
                                    (cdq.ui.hp-mana-bar/create [(/ (:width ctx/ui-viewport) 2)
                                                                80 ; action-bar-icon-size
                                                                ])
                                    (cdq.ui.windows/create :id :windows
                                                           :actors [(cdq.ui.entity-info/create [(:width ctx/ui-viewport) 0])
                                                                    (cdq.ui.inventory/create :id :inventory-window
                                                                                             :position [(:width  ctx/ui-viewport)
                                                                                                        (:height ctx/ui-viewport)])])
                                    (cdq.ui.player-state-draw/create)
                                    (cdq.ui.message/create :name "player-message")]))
  (input/set-processor! ctx/stage)
  (let [{:keys [tiled-map start-position]} ((requiring-resolve world-fn))
        width  (tiled/tm-width  tiled-map)
        height (tiled/tm-height tiled-map)]
    (bind-root #'ctx/tiled-map tiled-map)
    (bind-root #'ctx/grid (grid/create tiled-map))
    (bind-root #'ctx/raycaster (raycaster/create ctx/grid))
    (bind-root #'ctx/content-grid (content-grid/create {:cell-size (::content-grid-cells-size ctx/config)
                                                        :width  width
                                                        :height height}))
    (bind-root #'ctx/explored-tile-corners (atom (g2d/create-grid width
                                                                  height
                                                                  (constantly false))))
    (bind-root #'ctx/id-counter (atom 0))
    (bind-root #'ctx/entity-ids (atom {}))
    (bind-root #'ctx/potential-field-cache (atom nil))
    (handle-txs! (spawn-enemies tiled-map))
    (handle-txs! (spawn-player start-position))))

(defn create! []
  (bind-root #'ctx/config (create-config "config.edn"))
  (run! require (::requires ctx/config))
  (bind-root #'ctx/schemas (io-slurp-edn (::schemas ctx/config)))
  (bind-root #'ctx/db (db/create (::db ctx/config)))
  (bind-root #'ctx/assets (assets/create (::assets ctx/config)))
  (bind-root #'ctx/batch (graphics/sprite-batch))
  (bind-root #'ctx/shape-drawer-texture (graphics/white-pixel-texture))
  (bind-root #'ctx/shape-drawer (graphics/shape-drawer ctx/batch
                                                       (graphics/texture-region ctx/shape-drawer-texture 1 0 1 1)))
  (bind-root #'ctx/cursors (mapvals
                            (fn [[file [hotspot-x hotspot-y]]]
                              (graphics/cursor (format (::cursor-path-format ctx/config) file)
                                               hotspot-x
                                               hotspot-y))
                            (::cursors ctx/config)))
  (bind-root #'ctx/default-font (graphics/truetype-font (::default-font ctx/config)))
  (bind-root #'ctx/world-unit-scale (float (/ (::tile-size ctx/config))))
  (bind-root #'ctx/world-viewport (graphics/world-viewport ctx/world-unit-scale
                                                           (::world-viewport ctx/config)))
  (bind-root #'ctx/get-tiled-map-renderer (memoize (fn [tiled-map]
                                                     (tiled/renderer tiled-map
                                                                     ctx/world-unit-scale
                                                                     (:java-object ctx/batch)))))
  (bind-root #'ctx/ui-viewport (graphics/ui-viewport (::ui-viewport ctx/config)))
  (ui/load! (::ui ctx/config))
  (reset-game! (::tiled-map ctx/config)))

(defn dispose []
  (dispose! ctx/assets)
  (dispose! ctx/batch)
  (dispose! ctx/shape-drawer-texture)
  (run! dispose! (vals ctx/cursors))
  (dispose! ctx/default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn render! []
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

(defn resize! []
  (viewport/update! ctx/ui-viewport)
  (viewport/update! ctx/world-viewport))
