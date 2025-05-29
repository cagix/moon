(ns cdq.game
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.g.info]
            [cdq.g.player-movement-vector]
            [cdq.g.interaction-state]
            [cdq.g.spawn-entity]
            [cdq.g.spawn-creature]
            [cdq.g.handle-txs]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.grid-impl :as grid-impl]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.malli :as m]
            [cdq.state :as state]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.inventory :as inventory-window]
            [cdq.vector2 :as v]
            [clojure.gdx.interop :as interop]
            [gdl.assets :as assets]
            [gdl.files]
            [gdl.graphics]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils Disposable
                                   ScreenUtils)))

(defn- make-files []
  (let [files Gdx/files]
    (reify gdl.files/Files
      (internal [_ path]
        (.internal files path)))))

(defn- make-graphics []
  (let [graphics Gdx/graphics]
    (reify gdl.graphics/Graphics
      (new-cursor [_ pixmap hotspot-x hotspot-y]
        (.newCursor graphics pixmap hotspot-x hotspot-y))

      (delta-time [_]
        (.getDeltaTime graphics))

      (set-cursor! [_ cursor]
        (.setCursor graphics cursor))

      (frames-per-second [_]
        (.getFramesPerSecond graphics))

      (clear-screen! [_]
        (ScreenUtils/clear Color/BLACK)))))

(defn- make-input []
  (let [input Gdx/input]
    (reify gdl.input/Input
      (button-just-pressed? [_ button]
        (.isButtonJustPressed input (interop/k->input-button button)))

      (key-pressed? [_ key]
        (.isKeyPressed input (interop/k->input-key key)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed input (interop/k->input-key key)))

      (set-processor! [_ input-processor]
        (.setInputProcessor input input-processor))

      (mouse-position [_]
        [(.getX input)
         (.getY input)]))))

(defn- gdx-context []
  {;:clojure.gdx/app      Gdx/app
   :clojure.gdx/files    (make-files)
   :clojure.gdx/graphics (make-graphics)
   :clojure.gdx/input    (make-input)})

(q/defrecord Context [ctx/assets
                      ctx/graphics
                      ctx/stage])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/assets :some]
             [:ctx/graphics :some]
             [:ctx/input :some]
             [:ctx/ui-viewport :some]
             [:ctx/stage :some]
             [:ctx/config :some]
             [:ctx/db :some]
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

(defn- add-stage! [ctx input]
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:batch (:ctx/graphics ctx)))]
    (input/set-processor! input stage)
    (assoc ctx :ctx/stage stage)))

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

(defn- create-game-state [{:keys [ctx/config
                                  ctx/stage]
                           :as ctx}
                          world-fn]
  (ui/clear! stage)
  (run! #(ui/add! stage %) ((:create-actors config) ctx))
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
  g/MouseViewports
  (world-mouse-position [{:keys [ctx/graphics
                                 ctx/input]}]
    (viewport/unproject (:world-viewport graphics)
                        (input/mouse-position input)))

  (ui-mouse-position [{:keys [ctx/ui-viewport
                              ctx/input]}]
    (viewport/unproject ui-viewport
                        (input/mouse-position input))))

(extend-type Context
  g/Stage
  (mouseover-actor [{:keys [ctx/stage] :as ctx}]
    (ui/hit stage (g/ui-mouse-position ctx))))

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
  g/StageActors
  (open-error-window! [{:keys [ctx/stage]} throwable]
    (ui/add! stage (error-window/create throwable)))

  (selected-skill [{:keys [ctx/stage]}]
    (action-bar/selected-skill (:action-bar stage))))

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
  g/Game
  (reset-game-state! [ctx world-fn]
    (create-game-state ctx world-fn)))

(extend-type Context
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
(defn- on-screen? [graphics position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (graphics/camera-position graphics)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (graphics/world-viewport-width graphics))  2)))
     (<= ydist (inc (/ (float (graphics/world-viewport-height graphics)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type Context
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/graphics
                           ctx/raycaster]}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? graphics (entity/position target)))
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
  (player-movement-vector [{:keys [ctx/input]}]
    (cdq.g.player-movement-vector/WASD-movement-vector input)))

(extend-type Context
  g/InteractionState
  (interaction-state [ctx eid]
    (cdq.g.interaction-state/create ctx eid)))

(extend-type Context
  g/SpawnEntity
  (spawn-entity! [ctx
                  position
                  body
                  components]
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

(defn create! [config]
  (ui/load! (:ui config))
  (let [{:keys [clojure.gdx/input
                clojure.gdx/files] :as gdx} (gdx-context)
        ctx (-> (map->Context {})
                (assoc :ctx/config config)
                (assoc :ctx/graphics (graphics/create gdx config))
                (assoc :ctx/input input)
                (assoc :ctx/ui-viewport (viewport/ui-viewport (:ui-viewport config)))
                (add-stage! input)
                (assoc :ctx/assets (assets/create files (:assets config)))
                (assoc :ctx/db (db/create (:db config)))
                (create-game-state (:world-fn config)))]
    (m/validate-humanize schema ctx)
    ctx))

(defn dispose! [{:keys [ctx/assets
                        ctx/graphics]}]
  (Disposable/.dispose assets)
  (Disposable/.dispose graphics)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn resize! [{:keys [ctx/ui-viewport] :as ctx}
               width
               height]
  (viewport/update! ui-viewport width height)
  (viewport/update! (:world-viewport (:ctx/graphics ctx)) width height))

(def render-fns
  '[
    cdq.render.assoc-active-entities/do!
    cdq.render.set-camera-on-player/do!
    cdq.render.clear-screen/do!
    cdq.render.draw-world-map/do!
    cdq.render.draw-on-world-viewport/do!
    cdq.render.ui/do!
    cdq.render.player-state-handle-click/do!
    cdq.render.update-mouseover-entity/do!
    cdq.render.assoc-paused/do!
    cdq.render.update-time/do!
    cdq.render.update-potential-fields/do!
    cdq.render.tick-entities/do!
    cdq.render.remove-destroyed-entities/do!
    cdq.render.camera-controls/do!
    ])

(defn render! [ctx]
  (m/validate-humanize schema ctx)
  (let [render-fns (map requiring-resolve render-fns)
        ctx (reduce (fn [ctx render!]
                      (render! ctx))
                    ctx
                    render-fns)]
    (m/validate-humanize schema ctx)
    ctx))
