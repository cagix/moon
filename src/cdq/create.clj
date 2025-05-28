(ns cdq.create
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.timer :as timer]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.inventory :as inventory-window]
            [cdq.vector2 :as v]
            [clojure.gdx.input]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.graphics.color :as color]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport]))

(defn- make-input [input]
  (reify input/Input
    (button-just-pressed? [_ button]
      (clojure.gdx.input/button-just-pressed? input button))

    (key-pressed? [_ key]
      (clojure.gdx.input/key-pressed? input key))

    (key-just-pressed? [_ key]
      (clojure.gdx.input/key-just-pressed? input key))))

(defn- add-stage! [ctx input]
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:batch (:ctx/graphics ctx)))]
    (.setInputProcessor input stage)
    (assoc ctx :ctx/stage stage)))

(extend-type gdl.application.Context
  g/Stage
  (find-actor-by-name [{:keys [ctx/stage]} name]
    (-> stage
        ui/root
        (ui/find-actor name))) ; <- find-actor protocol & for stage use ui/root

  (mouseover-actor [{:keys [ctx/ui-viewport
                            ctx/stage]}]
    (ui/hit stage (viewport/mouse-position ui-viewport))))

(extend-type gdl.application.Context
  g/Context
  (context-entity-add! [{:keys [ctx/entity-ids
                                ctx/content-grid
                                ctx/grid]}
                        eid]
    (let [id (entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid/add-entity! grid eid))

  (context-entity-remove! [{:keys [ctx/entity-ids
                                   ctx/grid]}
                           eid]
    (let [id (entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid))

  (context-entity-moved! [{:keys [ctx/content-grid
                                  ctx/grid]}
                          eid]
    (content-grid/position-changed! content-grid eid)
    (grid/position-changed! grid eid)))

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
                                 :skill-added! (fn [{:keys [ctx/stage]} skill]
                                                 (action-bar/add-skill! (:action-bar stage)
                                                                        skill))
                                 :skill-removed! (fn [{:keys [ctx/stage]} skill]
                                                   (action-bar/remove-skill! (:action-bar stage)
                                                                             skill))
                                 :item-set! (fn [{:keys [ctx/stage]} inventory-cell item]
                                              (-> (:windows stage)
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [{:keys [ctx/stage]} inventory-cell]
                                                  (-> (:windows stage)
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position]
  (g/spawn-creature! ctx
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

(extend-type gdl.application.Context
  g/StageActors
  (open-error-window! [{:keys [ctx/stage]} throwable]
    (ui/add! stage (error-window/create throwable)))

  (selected-skill [{:keys [ctx/stage]}]
    (action-bar/selected-skill (:action-bar stage))))

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

(extend-type gdl.application.Context
  cdq.g/Grid
  (grid-cell [{:keys [ctx/grid]} position]
    (grid/cell grid position))

  (point->entities [{:keys [ctx/grid]} position]
    (grid/point->entities grid position))

  (valid-position? [{:keys [ctx/grid]} new-body]
    (grid/valid-position? grid new-body))

  (circle->cells [{:keys [ctx/grid]} circle]
    (grid/circle->cells grid circle))

  (circle->entities [{:keys [ctx/grid]} circle]
    (grid/circle->entities grid circle))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(defn- create-game-state [{:keys [ctx/config
                                  ctx/stage]
                           :as ctx}
                          world-fn]
  (ui/clear! stage)
  (run! #(ui/add! stage %) ((:create-actors config) ctx))
  (let [{:keys [tiled-map
                start-position]} (world-fn ctx)
        grid (grid-impl/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (raycaster/create grid)
                    :ctx/content-grid (content-grid/create tiled-map (:content-grid-cell-size config))
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
  g/Game
  (reset-game-state! [ctx world-fn]
    (create-game-state ctx world-fn)))

(defn create! [{:keys [clojure.gdx/input
                       clojure.gdx/files]
                :as gdx}
               config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create gdx config))
      (assoc :ctx/input (make-input input))
      (assoc :ctx/ui-viewport (viewport/ui-viewport (:ui-viewport config)))
      (add-stage! input)
      (assoc :ctx/assets (assets/create files (:assets config)))
      (assoc :ctx/db (db/create (:db config)))
      (g/reset-game-state! (:world-fn config))))

(extend-type gdl.application.Context
  g/Graphics
  (sprite [{:keys [ctx/assets] :as ctx} texture-path] ; <- textures should be inside graphics, makes this easier.
    (graphics/sprite (:ctx/graphics ctx)
                     (assets/texture assets texture-path)))

  (sub-sprite [ctx sprite [x y w h]]
    (graphics/sub-sprite (:ctx/graphics ctx)
                         sprite
                         [x y w h]))

  (sprite-sheet [{:keys [ctx/assets] :as ctx} texture-path tilew tileh]
    (graphics/sprite-sheet (:ctx/graphics ctx)
                           (assets/texture assets texture-path)
                           tilew
                           tileh))

  (sprite-sheet->sprite [ctx sprite-sheet [x y]]
    (graphics/sprite-sheet->sprite (:ctx/graphics ctx)
                                   sprite-sheet
                                   [x y])))

(extend-type gdl.application.Context
  g/ActiveEntities
  (get-active-entities [{:keys [ctx/content-grid
                                ctx/player-eid]}]
    (content-grid/active-entities content-grid @player-eid)))

(extend-type gdl.application.Context
  g/DrawWorldMap
  (draw-world-map! [{:keys [ctx/graphics
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
                                :see-all-tiles? false}))))

(extend-type gdl.application.Context
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/graphics
                              ctx/mouseover-eid]}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (entity/position @mouseover-eid))
                              (graphics/world-mouse-position graphics))]
      {:effect/source eid
       :effect/target mouseover-eid
       :effect/target-position target-position
       :effect/target-direction (v/direction (entity/position @eid) target-position)}))

  (npc-effect-ctx [ctx eid]
    (let [entity @eid
          target (g/nearest-enemy ctx entity)
          target (when (and target
                            (g/line-of-sight? ctx entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (entity/position entity)
                                               (entity/position @target)))})))

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
