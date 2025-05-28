(ns cdq.application-state
  (:require cdq.application
            [cdq.cell :as cell]
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
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.editor :as editor]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.windows]
            [cdq.ui.entity-info]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.player-state-draw]
            [cdq.ui.message]
            [clojure.string :as str]
            [gdl.application]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.ui.menu :as menu]
            [gdl.utils :as utils]
            [gdl.viewport :as viewport])
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)))

(declare create-game-state)

(defn create-dev-menu [{:keys [ctx/assets
                               ctx/config
                               ctx/db] :as ctx}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns config)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! cdq.application/state create-game-state world-fn))})}
            {:label "Help"
             :items [{:label (:info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (db/property-types db))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (assets/texture assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (g/elapsed-time ctx)) " seconds"))
                     :icon (assets/texture assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [{:keys [ctx/ui-viewport]}]
                                  (mapv int (viewport/mouse-position ui-viewport)))}
                    {:label "World"
                     :update-fn (fn [{:keys [ctx/graphics]}]
                                  (mapv int (graphics/world-mouse-position graphics)))}
                    {:label "Zoom"
                     :update-fn (comp graphics/camera-zoom :ctx/graphics)
                     :icon (assets/texture assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (comp graphics/frames-per-second :ctx/graphics)
                     :icon (assets/texture assets "images/fps.png")}]}))

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(defn- make-input [input]
  (reify input/Input
    (button-just-pressed? [_ button]
      (.isButtonJustPressed input (button->code button)))

    (key-pressed? [_ key]
      (.isKeyPressed input (k->code key)))

    (key-just-pressed? [_ key]
      (.isKeyJustPressed input (k->code key)))))

(defn- add-stage! [ctx]
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:batch (:ctx/graphics ctx)))]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx :ctx/stage stage)))

(defn- create-actors [{:keys [ctx/ui-viewport]
                       :as ctx}]
  [(create-dev-menu ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(extend-type gdl.application.Context
  g/Stage
  (find-actor-by-name [{:keys [ctx/stage]} name]
    (-> stage
        ui/root
        (ui/find-actor name))) ; <- find-actor protocol & for stage use ui/root

  (mouseover-actor [{:keys [ctx/ui-viewport
                            ctx/stage]}]
    (ui/hit stage (viewport/mouse-position ui-viewport)))

  (reset-actors! [{:keys [ctx/stage] :as ctx}]
    (ui/clear! stage)
    (run! #(ui/add! stage %) (create-actors ctx))))

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

(defn create-game-state [{:keys [ctx/config] :as ctx} world-fn]
  (g/reset-actors! ctx)
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

(defn create! [config]
  (ui/load! (:ui config))
  (-> (gdl.application/map->Context {})
      (assoc :ctx/config config)
      (assoc :ctx/graphics (graphics/create Gdx/graphics config)) ; <- actually create only called here <- all libgdx create stuff here and assets/input/graphics/stage/viewport as protocols in gdl ? -> all gdx code creating together and upfactored protocols?
      (assoc :ctx/input (make-input Gdx/input))
      (assoc :ctx/ui-viewport (viewport/ui-viewport (:ui-viewport config))) ; <- even viewport construction is in here .... viewport itself a protocol  ....
      (add-stage!)
      (assoc :ctx/assets (assets/create (:assets config)))
      (assoc :ctx/db (db/create (:db config)))
      (create-game-state (:world-fn config))))

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
