(ns cdq.game
  (:require [cdq.animation :as animation]
            [cdq.audio]
            [cdq.assets]
            [cdq.c :as c]
            [cdq.create.db]
            [cdq.create.ui]
            [cdq.ctx :as ctx]
            cdq.ctx.interaction-state
            [cdq.db :as db]
            [cdq.dev.data-view :as data-view]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            cdq.entity.alert-friendlies-after-duration
            cdq.entity.animation
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.movement
            cdq.entity.projectile-collision
            cdq.entity.skills
            cdq.entity.state.active-skill
            cdq.entity.state.npc-idle
            cdq.entity.state.npc-moving
            cdq.entity.state.npc-sleeping
            cdq.entity.state.stunned
            cdq.entity.string-effect
            cdq.entity.temp-modifier
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.entity.state.player-moving
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.input :as input]
            [cdq.malli :as m]
            [cdq.math.geom :as geom]
            [cdq.raycaster :as raycaster]
            [cdq.stacktrace :as stacktrace]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.timer :as timer]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.ui]
            [cdq.ui.stage :as stage]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.ui.player-state-draw
            cdq.ui.message
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.val-max :as val-max]
            [cdq.world :as world]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx.utils Disposable)))

(q/defrecord Context [ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/graphics
                      ctx/world])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/graphics :some]
             [:ctx/world :some]]))

(defn- validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)

(declare ^:private reset-game-state!)

(def ^:private state->clicked-inventory-cell
  {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-cell})

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ;graphics db
                           {:reset-game-state-fn reset-game-state!
                            :world-fns [[(requiring-resolve 'cdq.level.from-tmx/create)
                                         {:tmx-file "maps/vampire.tmx"
                                          :start-position [32 71]}]
                                        [(requiring-resolve 'cdq.level.uf-caves/create)
                                         {:tile-size 48
                                          :texture "maps/uf_terrain.png"
                                          :spawn-rate 0.02
                                          :scaling 3
                                          :cave-size 200
                                          :cave-style :wide}]
                                        [(requiring-resolve 'cdq.level.modules/create)
                                         {:world/map-size 5,
                                          :world/max-area-level 3,
                                          :world/spawn-rate 0.05}]]
                            ;icons, etc. , components ....
                            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"})
    (cdq.ui.action-bar/create {:id :action-bar}) ; padding.... !, etc.

    ; graphics
    (cdq.ui.hp-mana-bar/create ctx
                               {:rahmen-file "images/rahmen.png"
                                :rahmenw 150
                                :rahmenh 26
                                :hpcontent-file "images/hp.png"
                                :manacontent-file "images/mana.png"
                                :y-mana 80}) ; action-bar-icon-size

    {:actor/type :actor.type/group
     :id :windows
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0}) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :clicked-cell-fn (fn [cell]
                                   (fn [{:keys [ctx/world] :as ctx}]
                                     (ctx/handle-txs!
                                      ctx
                                      (let [player-eid (:world/player-eid world)]
                                        (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                                          (f player-eid cell))))))})]}
    (cdq.ui.player-state-draw/create
     {:state->draw-gui-view
      {:player-item-on-cursor
       cdq.entity.state.player-item-on-cursor/draw-gui-view}})
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])

(defn- reset-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (doseq [actor (create-ui-actors ctx)]
    (stage/add! stage actor))
  ctx)

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (world/create (merge (:cdq.ctx.game/world config)
                                             (let [[f params] world-fn]
                                               (f ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (world/spawn-creature! world)
       (ctx/handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc-in ctx [:ctx/world :world/player-eid] player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (world/spawn-creature! world)
         (ctx/handle-txs! ctx)))
  ctx)

(defn- reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(defn create! [{:keys [audio files graphics input]}]
  (let [graphics (graphics/create
                  graphics
                  files
                  {:colors [["PRETTY_NAME" [0.84 0.8 0.52 1]]]
                   :textures (cdq.assets/search files
                                                {:folder "resources/"
                                                 :extensions #{"png" "bmp"}})
                   :tile-size 48
                   :ui-viewport    {:width 1440 :height 900}
                   :world-viewport {:width 1440 :height 900}
                   :cursor-path-format "cursors/%s.png"
                   :cursors {:cursors/bag                   ["bag001"       [0   0]]
                             :cursors/black-x               ["black_x"      [0   0]]
                             :cursors/default               ["default"      [0   0]]
                             :cursors/denied                ["denied"       [16 16]]
                             :cursors/hand-before-grab      ["hand004"      [4  16]]
                             :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                             :cursors/hand-grab             ["hand003"      [4  16]]
                             :cursors/move-window           ["move002"      [16 16]]
                             :cursors/no-skill-selected     ["denied003"    [0   0]]
                             :cursors/over-button           ["hand002"      [0   0]]
                             :cursors/sandclock             ["sandclock"    [16 16]]
                             :cursors/skill-not-usable      ["x007"         [0   0]]
                             :cursors/use-skill             ["pointer004"   [0   0]]
                             :cursors/walking               ["walking"      [16 16]]}
                   :default-font {:file "exocet/films.EXL_____.ttf"
                                  :params {:size 16
                                           :quality-scaling 2
                                           :enable-markup? true
                                           ; false, otherwise scaling to world-units not visible
                                           :use-integer-positions? false}}})
        ctx (map->Context {:audio (cdq.audio/create audio files {:sounds "sounds.edn"})
                           :config {:cdq.ctx.game/enemy-components {:entity/fsm {:fsm :fsms/npc
                                                                                 :initial-state :npc-sleeping}
                                                                    :entity/faction :evil}
                                    :cdq.ctx.game/player-props {:creature-id :creatures/vampire
                                                                :components {:entity/fsm {:fsm :fsms/player
                                                                                          :initial-state :player-idle}
                                                                             :entity/faction :good
                                                                             :entity/player? true
                                                                             :entity/free-skill-points 3
                                                                             :entity/clickable {:type :clickable/player}
                                                                             :entity/click-distance-tiles 1.5}}
                                    :cdq.ctx.game/world {:content-grid-cell-size 16
                                                         :potential-field-factions-iterations {:good 15
                                                                                               :evil 5}}
                                    :effect-body-props {:width 0.5
                                                        :height 0.5
                                                        :z-order :z-order/effect}

                                    :controls {:zoom-in :minus
                                               :zoom-out :equals
                                               :unpause-once :p
                                               :unpause-continously :space}}
                           :db (cdq.create.db/do! {:schemas "schema.edn"
                                                   :properties "properties.edn"})
                           :graphics graphics
                           :input input
                           :stage (cdq.create.ui/do! graphics input {:skin-scale :x1})})]
    (-> ctx
        (reset-game-state! [(requiring-resolve 'cdq.level.from-tmx/create)
                            {:tmx-file "maps/vampire.tmx"
                             :start-position [32 71]}])
        validate)))

; TODO call dispose! on all components
(defn dispose! [{:keys [ctx/audio
                        ctx/graphics
                        ctx/world]}]
  (Disposable/.dispose audio)
  (Disposable/.dispose graphics)
  (Disposable/.dispose (:world/tiled-map world))
  ; TODO vis-ui dispose
  ; TODO what else disposable?
  ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
  )

; TODO call resize! on all components
(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))

(defn- check-open-debug-data-view!
  [{:keys [ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [mouseover-eid (:world/mouseover-eid world)
          data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int (cdq.c/world-mouse-position ctx))))]
      (stage/add! stage (data-view/table-view-window {:title "Data View"
                                                      :data data
                                                      :width 500
                                                      :height 500}))))
  ctx)

(defn- assoc-active-entities [ctx]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/world
           ctx/graphics]
    :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (entity/position @(:world/player-eid world)))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics :black)
  ctx)

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter/create
                             {:ray-blocked? (let [raycaster (:world/raycaster world)]
                                              (fn [start end] (raycaster/blocked? raycaster start end)))
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (:camera/position (:viewport/camera (:world-viewport graphics)))
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:viewport/camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:viewport/width world-viewport)))
      (+ 2 (int (:viewport/height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics] :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* (:world-viewport graphics)))))

(defn- draw-cell-debug* [{:keys [ctx/world
                                 ctx/graphics]}]
  (let [grid (:world/grid world)]
    (apply concat
           (for [[x y] (camera/visible-tiles (:viewport/camera (:world-viewport graphics)))
                 :let [cell (grid/cell grid [x y])]
                 :when cell
                 :let [cell* @cell]]
             [(when (and show-cell-entities? (seq (:entities cell*)))
                [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
              (when (and show-cell-occupied? (seq (:occupied cell*)))
                [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
              (when-let [faction show-potential-field-colors?]
                (let [{:keys [distance]} (faction cell*)]
                  (when distance
                    (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                      [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))]))))

(defn- draw-cell-debug [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(def ^:private mouseover-ellipse-width 5)

(def ^:private stunned-circle-width 0.5)
(def ^:private stunned-circle-color [1 1 1 0.6])

(defn- draw-item-on-cursor-state [{:keys [item]} entity ctx]
  (when (cdq.entity.state.player-item-on-cursor/world-item? ctx)
    [[:draw/centered
      (:entity/image item)
      (cdq.entity.state.player-item-on-cursor/item-place-position ctx entity)]]))

(defn- draw-mouseover-highlighting [_ entity {:keys [ctx/world]}]
  (let [player @(:world/player-eid world)
        faction (entity/faction entity)]
    [[:draw/with-line-width mouseover-ellipse-width
      [[:draw/ellipse
        (entity/position entity)
        (/ (:body/width  (:entity/body entity)) 2)
        (/ (:body/height (:entity/body entity)) 2)
        (cond (= faction (entity/enemy player))
              enemy-color
              (= faction (entity/faction player))
              friendly-color
              :else
              neutral-color)]]]]))

(defn- draw-stunned-state [_ entity _ctx]
  [[:draw/circle
    (entity/position entity)
    stunned-circle-width
    stunned-circle-color]])

(defn- draw-clickable-mouseover-text [{:keys [text]} {:keys [entity/mouseover?] :as entity} _ctx]
  (when (and mouseover? text)
    (let [[x y] (entity/position entity)]
      [[:draw/text {:text text
                    :x x
                    :y (+ y (/ (:body/height (:entity/body entity)) 2))
                    :up? true}]])))

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-skill-image [image entity [x y] action-counter-ratio]
  (let [radius skill-image-radius-world-units
        y (+ (float y)
             (float (/ (:body/height (:entity/body entity)) 2))
             (float 0.15))
        center [x (+ y radius)]]
    [[:draw/filled-circle center radius [1 1 1 0.125]]
     [:draw/sector
      center
      radius
      90 ; start-angle
      (* (float action-counter-ratio) 360) ; degree
      [1 1 1 0.5]]
     [:draw/image image [(- (float x) radius) y]]]))

(defn- render-active-effect [ctx effect-ctx effect]
  (mapcat #(effect/render % effect-ctx ctx) effect))

(defn- draw-active-skill [{:keys [skill effect-ctx counter]}
                         entity
                         {:keys [ctx/world] :as ctx}]
  (let [{:keys [entity/image skill/effects]} skill]
    (concat (draw-skill-image image
                              entity
                              (entity/position entity)
                              (timer/ratio (:world/elapsed-time world) counter))
            (render-active-effect ctx
                                  effect-ctx ; TODO !!!
                                  ; !! FIXME !!
                                  ; (update-effect-ctx effect-ctx)
                                  ; - render does not need to update .. update inside active-skill
                                  effects))))

(defn- draw-centered-rotated-image [image entity _ctx]
  [[:draw/rotated-centered
    image
    (or (:body/rotation-angle (:entity/body entity)) 0)
    (entity/position entity)]])

(defn- call-render-image [animation entity ctx]
  (draw-centered-rotated-image (animation/current-frame animation)
                               entity
                               ctx))

(defn- draw-line-entity [{:keys [thick? end color]} entity _ctx]
  (let [position (entity/position entity)]
    (if thick?
      [[:draw/with-line-width 4 [[:draw/line position end color]]]]
      [[:draw/line position end color]])))

(defn- draw-sleeping-state [_ entity _ctx]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text "zzz"
                  :x x
                  :y (+ y (/ (:body/height (:entity/body entity)) 2))
                  :up? true}]]))

; TODO draw opacity as of counter ratio?
(defn- draw-temp-modifiers [_ entity _ctx]
  [[:draw/filled-circle (entity/position entity) 0.5 [0.5 0.5 0.5 0.4]]])

(defn- draw-text-over-entity [{:keys [text]} entity {:keys [ctx/graphics]}]
  (let [[x y] (entity/position entity)]
    [[:draw/text {:text text
                  :x x
                  :y (+ y
                        (/ (:body/height (:entity/body entity)) 2)
                        (* 5 (:world-unit-scale graphics)))
                  :scale 2
                  :up? true}]]))

(def ^:private hpbar-colors
  {:green     [0 0.8 0 1]
   :darkgreen [0 0.5 0 1]
   :yellow    [0.5 0.5 0 1]
   :red       [0.5 0 0 1]})

(defn- hpbar-color [ratio]
  (let [ratio (float ratio)
        color (cond
               (> ratio 0.75) :green
               (> ratio 0.5)  :darkgreen
               (> ratio 0.25) :yellow
               :else          :red)]
    (color hpbar-colors)))

(def ^:private borders-px 1)

(defn- draw-hpbar [world-unit-scale {:keys [body/position body/width body/height]} ratio]
  (let [[x y] position]
    (let [x (- x (/ width  2))
          y (+ y (/ height 2))
          height (* 5          world-unit-scale)
          border (* borders-px world-unit-scale)]
      [[:draw/filled-rectangle x y width height :black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(defn- draw-stats [_ entity {:keys [ctx/graphics]}]
  (let [ratio (val-max/ratio (entity/hitpoints entity))] ; <- use stats directly?
    (when (or (< ratio 1) (:entity/mouseover? entity))
      (draw-hpbar (:world-unit-scale graphics)
                  (:entity/body entity)
                  ratio))))

(def ^:private render-below {:entity/mouseover? draw-mouseover-highlighting
                             :stunned draw-stunned-state
                             :player-item-on-cursor draw-item-on-cursor-state})

(def ^:private render-default {:entity/clickable draw-clickable-mouseover-text
                               :entity/animation call-render-image
                               :entity/image draw-centered-rotated-image
                               :entity/line-render draw-line-entity})

(def ^:private render-above {:npc-sleeping draw-sleeping-state
                             :entity/temp-modifier draw-temp-modifiers
                             :entity/string-effect draw-text-over-entity})

(def ^:private render-info {:creature/stats draw-stats
                            :active-skill draw-active-skill})

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-entity [{:keys [ctx/graphics] :as ctx} entity render-layer]
  (try
   (when show-body-bounds?
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) (if (:body/collides? (:entity/body entity)) :white :gray))))
   (doseq [[k v] entity
           :let [draw-fn (get render-layer k)]
           :when draw-fn]
     (graphics/handle-draws! graphics (draw-fn v entity ctx)))
   (catch Throwable t
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) :red))
     (stacktrace/pretty-print t))))

(defn- render-entities
  [{:keys [ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer [render-below
                          render-default
                          render-above
                          render-info]
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(defn- geom-test* [{:keys [ctx/world] :as ctx}]
  (let [grid (:world/grid world)
        position (c/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world] :as ctx}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int (c/world-mouse-position ctx))
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f [draw-tile-grid
                                                 draw-cell-debug
                                                 render-entities
                                                 ; geom-test
                                                 highlight-mouseover-tile]]
                                        (f ctx))))
  ctx)

(defn- render-stage! [{:keys [ctx/stage] :as ctx}]
  (stage/render! stage ctx))

(def ^:private state->cursor
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle cdq.ctx.interaction-state/->cursor
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)]
    (graphics/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                                     (if (keyword? ->cursor)
                                       ->cursor
                                       (->cursor player-eid ctx)))))
  ctx)

(def ^:private state->handle-input
  {:player-idle           cdq.entity.state.player-idle/handle-input
   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/handle-input
   :player-moving         cdq.entity.state.player-moving/handle-input})

(defn- player-state-handle-input!
  [{:keys [ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)
        handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (ctx/handle-txs! ctx txs))
  ctx)

(defn- update-mouseover-entity!
  [{:keys [ctx/world]
    :as ctx}]
  (let [new-eid (if (c/mouseover-actor ctx)
                  nil
                  (let [player @(:world/player-eid world)
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities (:world/grid world) (c/world-mouse-position ctx)))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when-let [eid (:world/mouseover-eid world)]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/world
           ctx/config]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (let [controls (:controls config)]
              (or #_error
                  (and pausing?
                       (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                       (not (or (input/key-just-pressed? input (:unpause-once controls))
                                (input/key-pressed?      input (:unpause-continously controls)))))))))

(defn- update-time [{:keys [ctx/graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world world/update-time (graphics/delta-time graphics)))

(defn- tick-potential-fields!
  [{:keys [world/factions-iterations
           world/potential-field-cache
           world/grid
           world/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (tick-potential-fields! world)
  ctx)

(def ^:private entity->tick
  {:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick!
   :entity/animation cdq.entity.animation/tick!
   :entity/delete-after-animation-stopped? cdq.entity.delete-after-animation-stopped/tick!
   :entity/delete-after-duration cdq.entity.delete-after-duration/tick!
   :entity/movement cdq.entity.movement/tick!
   :entity/projectile-collision cdq.entity.projectile-collision/tick!
   :entity/skills cdq.entity.skills/tick!
   :active-skill cdq.entity.state.active-skill/tick!
   :npc-idle cdq.entity.state.npc-idle/tick!
   :npc-moving cdq.entity.state.npc-moving/tick!
   :npc-sleeping cdq.entity.state.npc-sleeping/tick!
   :stunned cdq.entity.state.stunned/tick!
   :entity/string-effect cdq.entity.string-effect/tick!
   :entity/temp-modifier cdq.entity.temp-modifier/tick!})

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (when-let [f (entity->tick k)]
                                  (f v eid world))))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (entity/id @eid)}
                           t))))))
(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (try
   (doseq [eid (:world/active-entities world)]
     (tick-entity! ctx eid))
   (catch Throwable t
     (stacktrace/pretty-print t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)

(defn- tick-world!
  [ctx]
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))

(defn- remove-destroyed-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (ctx/handle-txs! ctx (world/remove-entity! world eid)))
  ctx)

(def ^:private zoom-speed 0.025)

(defn- check-camera-controls!
  [{:keys [ctx/config
                   ctx/input
                   ctx/graphics]
            :as ctx}]
  (let [controls (:controls config)
        camera (:viewport/camera (:world-viewport graphics))]
    (when (input/key-pressed? input (:zoom-in controls))  (camera/inc-zoom! camera    zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (camera/inc-zoom! camera (- zoom-speed))))
  ctx)

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)

(defn- check-window-hotkeys!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/key-just-pressed? input close-windows-key)  (cdq.ui/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (cdq.ui/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (cdq.ui/toggle-entity-info-window! stage))
  ctx)

(defn render! [ctx]
  (-> ctx
      validate
      check-open-debug-data-view! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      render-stage!
      set-cursor!
      player-state-handle-input!
      update-mouseover-entity!
      assoc-paused
      tick-world!
      remove-destroyed-entities! ; do not pause as pickup item should be destroyed
      check-camera-controls!
      check-window-hotkeys!
      validate))
