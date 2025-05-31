(ns cdq.game
  (:require [cdq.resizable]
            [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.g.info]
            [cdq.g.player-movement-vector]
            [cdq.g.interaction-state]
            [cdq.g.spawn-entity]
            [cdq.g.spawn-creature]
            [cdq.g.handle-txs]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.state :as state]
            [cdq.ui.editor]
            [cdq.ui.editor.overview-table]
            [cdq.vector2 :as v]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.application :as application]
            [gdl.tiled :as tiled]
            [gdl.utils :as utils]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/assets
                      ctx/batch
                      ctx/config
                      ctx/cursors
                      ctx/db
                      ctx/default-font
                      ctx/graphics
                      ctx/input
                      ctx/stage
                      ctx/ui-viewport
                      ctx/unit-scale
                      ctx/shape-drawer
                      ctx/shape-drawer-texture
                      ctx/tiled-map-renderer
                      ctx/world-unit-scale
                      ctx/world-viewport])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
             [:ctx/batch :some]
             [:ctx/config :some]
             [:ctx/cursors :some]
             [:ctx/db :some]
             [:ctx/default-font :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/stage :some]
             [:ctx/ui-viewport :some]
             [:ctx/unit-scale :some]
             [:ctx/shape-drawer :some]
             [:ctx/shape-drawer-texture :some]
             [:ctx/tiled-map-renderer :some]
             [:ctx/world-unit-scale :some]
             [:ctx/world-viewport :some]
             [:ctx/elapsed-time :some]
             [:ctx/delta-time {:optional true} number?]
             [:ctx/paused? {:optional true} :boolean]
             [:ctx/tiled-map :some]
             [:ctx/grid :some]
             [:ctx/raycaster :some]
             [:ctx/content-grid :some]
             [:ctx/explored-tile-corners :some]
             [:ctx/id-counter :some]
             [:ctx/entity-ids :some]
             [:ctx/potential-field-cache :some]
             [:ctx/factions-iterations :some]
             [:ctx/z-orders :some]
             [:ctx/render-z-order :some]
             [:ctx/mouseover-eid {:optional true} :any]
             [:ctx/player-eid :some]
             [:ctx/active-entities {:optional true} :some]]))

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
                                 :skill-added! g/add-skill!
                                 :skill-removed! g/remove-skill!
                                 :item-set! g/set-item!
                                 :item-removed! g/remove-item!}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position player-props]
  (g/spawn-creature! ctx
                     (player-entity-props (utils/tile->middle start-position)
                                          player-props)))

(defn- spawn-enemies [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- create-game-state [{:keys [ctx/config] :as ctx} world-fn]
  (g/reset-actors! ctx)
  (let [{:keys [tiled-map
                start-position]} (world-fn ctx)
        grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
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
                    :ctx/potential-field-cache (atom nil)
                    :ctx/factions-iterations (:potential-field-factions-iterations config)
                    :ctx/z-orders z-orders
                    :ctx/render-z-order (utils/define-order z-orders)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx
                                                            start-position
                                                            (:player-props config)))]
    (g/handle-txs! ctx (spawn-enemies tiled-map))
    ctx))

(extend-type Context
  g/Game
  (reset-game-state! [ctx world-fn]
    (create-game-state ctx world-fn)))

; We _need to create stepwise because otherwise the file becomes too big ... _

(defn- create! [config]
  (let [ctx (map->Context {:config config})
        ctx (reduce (fn [ctx f]
                      (f ctx))
                    ctx
                    (map requiring-resolve
                         '[cdq.create.assets/do!
                           cdq.create.input/do!
                           cdq.create.db/do!
                           cdq.create.graphics/do!
                           cdq.create.stage/do!
                           ]))
        ctx (create-game-state ctx (:world-fn config))]
    (m/validate-humanize schema ctx)
    ctx))

(defn- dispose! [{:keys [ctx/assets
                         ctx/batch
                         ctx/cursors
                         ctx/default-font
                         ctx/shape-drawer-texture]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose batch)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  (Disposable/.dispose shape-drawer-texture)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(def render-fns
  '[
    cdq.render.assoc-active-entities/do!
    cdq.render.set-camera-on-player/do!
    cdq.render.clear-screen/do!
    cdq.render.draw-world-map/do!
    cdq.render.draw-on-world-viewport/do!
    cdq.g/render-stage!
    cdq.render.player-state-handle-click/do!
    cdq.render.update-mouseover-entity/do!
    cdq.render.assoc-paused/do!
    cdq.render.update-time/do!
    cdq.render.update-potential-fields/do!
    cdq.render.tick-entities/do!
    cdq.render.remove-destroyed-entities/do!
    cdq.render.camera-controls/do!
    ])

(defn- render! [ctx]
  (m/validate-humanize schema ctx)
  (let [render-fns (map requiring-resolve render-fns)
        ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    render-fns)]
    (m/validate-humanize schema ctx)
    ctx))

(defn- resize! [{:keys [ctx/ui-viewport
                        ctx/world-viewport]}
                width
                height]
  (cdq.resizable/resize! ui-viewport    width height)
  (cdq.resizable/resize! world-viewport width height))

(defn -main [config-path]
  (let [config (utils/create-config config-path)]
    (lwjgl/application (:lwjgl-application config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! application/state (create! config)))

                         (dispose []
                           (dispose! @application/state))

                         (render []
                           (swap! application/state render!))

                         (resize [width height]
                           (resize! @application/state width height))))))

(extend-type Context
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

(extend-type Context
  cdq.g/Grid
  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(extend-type Context
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/mouseover-eid]
                       :as ctx}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (entity/position @mouseover-eid))
                              (g/world-mouse-position ctx))]
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

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [ctx position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (g/camera-position ctx)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (g/world-viewport-width  ctx))  2)))
     (<= ydist (inc (/ (float (g/world-viewport-height ctx)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type Context
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/raycaster] :as ctx}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? ctx (entity/position target)))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (entity/position source)
                                       (entity/position target)))))))
(extend-type Context
  g/InfoText
  (info-text [ctx object]
    (cdq.g.info/text ctx object)))

(extend-type Context
  g/PlayerMovementInput
  (player-movement-vector [ctx]
    (cdq.g.player-movement-vector/WASD-movement-vector ctx)))

(extend-type Context
  g/InteractionState
  (interaction-state [ctx eid]
    (cdq.g.interaction-state/create ctx eid)))

(extend-type Context
  g/SpawnEntity
  (spawn-entity! [ctx position body components]
    (cdq.g.spawn-entity/spawn-entity! ctx position body components)))

(extend-type Context
  g/EffectHandler
  (handle-txs! [ctx transactions]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  ]]
      (try (cdq.g.handle-txs/handle-tx! transaction ctx)
           (catch Throwable t
             (throw (ex-info "" {:transaction transaction} t)))))))

(extend-type Context
  g/Creatures
  (spawn-creature! [ctx opts]
    (cdq.g.spawn-creature/spawn-creature! ctx opts)))

(extend-type Context
  g/EditorWindow
  (open-editor-window! [ctx property-type]
    (cdq.ui.editor/open-editor-window! ctx property-type))

  (edit-property! [ctx property]
    (g/add-actor! ctx (cdq.ui.editor/editor-window property ctx)))

  (property-overview-table [ctx property-type clicked-id-fn]
    (cdq.ui.editor.overview-table/create ctx property-type clicked-id-fn)))
