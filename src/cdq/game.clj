(ns cdq.game
  (:require [cdq.audio :as audio]
            [cdq.db :as db]
            [cdq.effects.target-all :as target-all]
            [cdq.effects.target-entity :as target-entity]
            [cdq.entity.stats :as stats]
            [cdq.graphics :as graphics]
            [cdq.input :as input]
            [cdq.ui :as ui]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.group :as build-group]
            [cdq.ui.stack :as stack]
            [cdq.ui.table :as table]
            [cdq.ui.dev-menu :as dev-menu]
            [cdq.ui.editor.overview-window :as editor-overview-window]
            [cdq.ui.editor.window :as editor-window]
            [cdq.ui.message :as message]
            [cdq.ui.stage :as stage]
            [cdq.world :as world]
            [cdq.world.info :as info]
            [cdq.world.raycaster :as raycaster]
            [cdq.world.tiled-map :as tiled-map]
            [cdq.world-fns.creature-tiles]
            [cdq.entity.animation :as animation]
            [cdq.entity.body :as body]
            [cdq.entity.faction :as faction]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.entity.state.player-item-on-cursor :as player-item-on-cursor]
            [cdq.entity.stats :as stats]
            [cdq.entity.skills.skill :as skill]
            [clojure.color :as color]
            [clojure.gdx.graphics.color :as gdxcolor]
            [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.math.vector2 :as gdxvector2]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.event :as event]
            [clojure.gdx.scene2d.ui.widget :as widget]
            [clojure.gdx.scene2d.utils.click-listener :as click-listener]
            [clojure.gdx.scene2d.utils.drawable :as drawable]
            [clojure.gdx.scene2d.utils.texture-region-drawable :as texture-region-drawable]
            [cdq.ui.info-window :as info-window]
            [cdq.ui.image :as image]
            [cdq.ui.window :as window]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.math.vector2 :as v]
            [clojure.string :as str]
            [clojure.timer :as timer]
            [clojure.throwable :as throwable]
            [clojure.txs :as txs]
            [clojure.tx-handler :as tx-handler]
            [clojure.utils :as utils]
            [clojure.val-max :as val-max]
            [malli.core :as m]
            [malli.utils :as mu]
            [qrecord.core :as q])
  (:import (cdq.ui Stage)))

(q/defrecord Context [])

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/world :some]]))

(defn- validate [ctx]
  (mu/validate-humanize schema ctx)
  ctx)

(declare rebuild-actors!
         create-world)

(defn- create-dev-menu
  [{:keys [ctx/db
           ctx/graphics]}]
  (let [open-editor (fn [db]
                      {:label "Editor"
                       :items (for [property-type (sort (db/property-types db))]
                                {:label (str/capitalize (name property-type))
                                 :on-click (fn [_actor {:keys [ctx/db
                                                               ctx/graphics
                                                               ctx/stage]}]
                                             (stage/add-actor!
                                              stage
                                              (editor-overview-window/create
                                               {:db db
                                                :graphics graphics
                                                :property-type property-type
                                                :clicked-id-fn (fn [_actor id {:keys [ctx/stage] :as ctx}]
                                                                 (stage/add-actor! stage
                                                                                   (editor-window/create-editor-window
                                                                                    {:ctx ctx
                                                                                     :property (db/get-raw db id)})))})))})})
        ctx-data-viewer {:label "Ctx Data"
                         :items [{:label "Show data"
                                  :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                                              (ui/show-data-viewer! stage ctx))}]}
        help-info-text {:label "Help"
                        :items [{:label input/info-text}]}
        select-world {:label "Select World"
                      :items (for [world-fn ["world_fns/vampire.edn"
                                             "world_fns/uf_caves.edn"
                                             "world_fns/modules.edn"]]
                               {:label (str "Start " world-fn)
                                :on-click (fn [actor {:keys [ctx/stage] :as ctx}]
                                            (let [ui stage
                                                  stage (actor/stage actor)]  ; get before clear, otherwise the actor does not have a stage anymore
                                              (rebuild-actors! ui ctx)
                                              (world/dispose! (:ctx/world ctx))
                                              (stage/set-ctx! stage (create-world ctx world-fn))))})}
        update-labels [{:label "elapsed-time"
                        :update-fn (fn [ctx]
                                     (str (utils/readable-number (:world/elapsed-time (:ctx/world ctx))) " seconds"))
                        :icon "images/clock.png"}
                       {:label "FPS"
                        :update-fn (fn [ctx]
                                     (graphics/frames-per-second (:ctx/graphics ctx)))
                        :icon "images/fps.png"}
                       {:label "Mouseover-entity id"
                        :update-fn (fn [{:keys [ctx/world]}]
                                     (let [eid (:world/mouseover-eid world)]
                                       (when-let [entity (and eid @eid)]
                                         (:entity/id entity))))
                        :icon "images/mouseover.png"}
                       {:label "paused?"
                        :update-fn (comp :world/paused? :ctx/world)}
                       {:label "GUI"
                        :update-fn (fn [{:keys [ctx/graphics]}]
                                     (mapv int (:graphics/ui-mouse-position graphics)))}
                       {:label "World"
                        :update-fn (fn [{:keys [ctx/graphics]}]
                                     (mapv int (:graphics/world-mouse-position graphics)))}
                       {:label "Zoom"
                        :update-fn (fn [ctx]
                                     (graphics/zoom (:ctx/graphics ctx)))
                        :icon "images/zoom.png"}]]
    {:menus [ctx-data-viewer
             (open-editor db)
             help-info-text
             select-world]
     :update-labels (for [item update-labels]
                      (if (:icon item)
                        (update item :icon #(get (:graphics/textures graphics) %))
                        item))}))

(defn- create-hp-mana-bar* [create-draws]
  (actor/create
   {:act (fn [_this _delta])
    :draw (fn [actor _batch _parent-alpha]
            (when-let [stage (actor/stage actor)]
              (graphics/draw! (:ctx/graphics (stage/ctx stage))
                              (create-draws (stage/ctx stage)))))}))

(let [config {:rahmen-file "images/rahmen.png"
              :rahmenw 150
              :rahmenh 26
              :hpcontent-file "images/hp.png"
              :manacontent-file "images/mana.png"
              :y-mana 80}]
  (defn- create-hp-mana-bar
    [{:keys [ctx/stage
             ctx/graphics]}]
    (let [{:keys [rahmen-file
                  rahmenw
                  rahmenh
                  hpcontent-file
                  manacontent-file
                  y-mana]} config
          [x y-mana] [(/ (ui/viewport-width stage) 2)
                      y-mana]
          rahmen-tex-reg (graphics/texture-region graphics {:image/file rahmen-file})
          y-hp (+ y-mana rahmenh)
          render-hpmana-bar (fn [x y content-file minmaxval name]
                              [[:draw/texture-region rahmen-tex-reg [x y]]
                               [:draw/texture-region
                                (graphics/texture-region graphics
                                                         {:image/file content-file
                                                          :image/bounds [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh]})
                                [x y]]
                               [:draw/text {:text (str (utils/readable-number (minmaxval 0))
                                                       "/"
                                                       (minmaxval 1)
                                                       " "
                                                       name)
                                            :x (+ x 75)
                                            :y (+ y 2)
                                            :up? true}]])]
      (fn [{:keys [ctx/world]}]
        (let [stats (:entity/stats @(:world/player-eid world))
              x (- x (/ rahmenw 2))]
          (concat
           (render-hpmana-bar x y-hp   hpcontent-file   (stats/get-hitpoints stats) "HP")
           (render-hpmana-bar x y-mana manacontent-file (stats/get-mana      stats) "MP")))))))

(defn- create-entity-info-window
  [{:keys [ctx/stage]}]
  {:title "Entity Info"
   :actor-name "cdq.ui.windows.entity-info"
   :visible? false
   :position [(ui/viewport-width stage) 0]
   :set-label-text! (fn [{:keys [ctx/world]}]
                      (if-let [eid (:world/mouseover-eid world)]
                        (info/text (apply dissoc @eid [:entity/skills
                                                       :entity/faction
                                                       :active-skill])
                                   world)
                        ""))})

(let [fn-map {:player-idle           (fn [eid cell]
                                       (when-let [item (get-in (:entity/inventory @eid) cell)]
                                         [[:tx/sound "bfxr_takeit"]
                                          [:tx/event eid :pickup-item item]
                                          [:tx/remove-item eid cell]]))

              :player-item-on-cursor (fn [eid cell]
                                       (let [entity @eid
                                             inventory (:entity/inventory entity)
                                             item-in-cell (get-in inventory cell)
                                             item-on-cursor (:entity/item-on-cursor entity)]
                                         (cond
                                          ; PUT ITEM IN EMPTY CELL
                                          (and (not item-in-cell)
                                               (inventory/valid-slot? cell item-on-cursor))
                                          [[:tx/sound "bfxr_itemput"]
                                           [:tx/dissoc eid :entity/item-on-cursor]
                                           [:tx/set-item eid cell item-on-cursor]
                                           [:tx/event eid :dropped-item]]

                                          ; STACK ITEMS
                                          (and item-in-cell
                                               (inventory/stackable? item-in-cell item-on-cursor))
                                          [[:tx/sound "bfxr_itemput"]
                                           [:tx/dissoc eid :entity/item-on-cursor]
                                           [:tx/stack-item eid cell item-on-cursor]
                                           [:tx/event eid :dropped-item]]

                                          ; SWAP ITEMS
                                          (and item-in-cell
                                               (inventory/valid-slot? cell item-on-cursor))
                                          [[:tx/sound "bfxr_itemput"]
                                           ; need to dissoc and drop otherwise state enter does not trigger picking it up again
                                           ; TODO? coud handle pickup-item from item-on-cursor state also
                                           [:tx/dissoc eid :entity/item-on-cursor]
                                           [:tx/remove-item eid cell]
                                           [:tx/set-item eid cell item-on-cursor]
                                           [:tx/event eid :dropped-item]
                                           [:tx/event eid :pickup-item item-in-cell]])))}]
  (defn state->clicked-inventory-cell [[k v] eid cell]
    (when-let [f (k fn-map)]
      (f eid cell))))

(defn- draw-cell-rect-actor [draw-cell-rect]
  (widget/create
    (fn [this _batch _parent-alpha]
      (when-let [stage (actor/stage this)]
        (let [{:keys [ctx/graphics
                      ctx/world]} (stage/ctx stage)]
          (graphics/draw! graphics
                          (let [ui-mouse (:graphics/ui-mouse-position graphics)]
                            (draw-cell-rect @(:world/player-eid world)
                                            (actor/x this)
                                            (actor/y this)
                                            (let [[x y] (-> this
                                                            (actor/stage->local-coordinates (gdxvector2/->java ui-mouse))
                                                            gdxvector2/->clj)]
                                              (actor/hit this x y true))
                                            (actor/user-object (actor/parent this))))))))))

(defn- create-inventory-window*
  [{:keys [position
           title
           actor/visible?
           clicked-cell-listener
           slot->texture-region]}]
  (let [cell-size 48
        slot->drawable (fn [slot]
                         (doto (texture-region-drawable/create (slot->texture-region slot))
                           (drawable/set-min-size! cell-size cell-size)
                           (texture-region-drawable/tint (gdxcolor/create [1 1 1 0.4]))))
        droppable-color   [0   0.6 0 0.8 1]
        not-allowed-color [0.6 0   0 0.8 1]
        draw-cell-rect (fn [player-entity x y mouseover? cell]
                         [[:draw/rectangle x y cell-size cell-size [0.5 0.5 0.5 1]]
                          (when (and mouseover?
                                     (= :player-item-on-cursor (:state (:entity/fsm player-entity))))
                            (let [item (:entity/item-on-cursor player-entity)
                                  color (if (inventory/valid-slot? cell item)
                                          droppable-color
                                          not-allowed-color)]
                              [:draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color]))])
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor (stack/create
                            {:actor/name "inventory-cell"
                             :actor/user-object cell
                             :actor/listener (clicked-cell-listener cell)
                             :group/actors [(draw-cell-rect-actor draw-cell-rect)
                                            (image/create
                                             {:image/object background-drawable
                                              :actor/name "image-widget"
                                              :actor/user-object {:background-drawable background-drawable
                                                                  :cell-size cell-size}})]})}))]
    (window/create
     {:title title
      :actor/name "cdq.ui.windows.inventory"
      :actor/visible? visible?
      :pack? true
      :actor/position position
      :rows [[{:actor (table/create
                       {:actor/name "inventory-cell-table"
                        :rows (concat [[nil nil
                                        (->cell :inventory.slot/helm)
                                        (->cell :inventory.slot/necklace)]
                                       [nil
                                        (->cell :inventory.slot/weapon)
                                        (->cell :inventory.slot/chest)
                                        (->cell :inventory.slot/cloak)
                                        (->cell :inventory.slot/shield)]
                                       [nil nil
                                        (->cell :inventory.slot/leg)]
                                       [nil
                                        (->cell :inventory.slot/glove)
                                        (->cell :inventory.slot/rings :position [0 0])
                                        (->cell :inventory.slot/rings :position [1 0])
                                        (->cell :inventory.slot/boot)]]
                                      (for [y (range 4)]
                                        (for [x (range 6)]
                                          (->cell :inventory.slot/bag :position [x y]))))})
               :pad 4}]]})))

(defn- create-inventory-window
  [{:keys [ctx/graphics
           ctx/stage]}]
  (let [slot->y-sprite-idx #:inventory.slot {:weapon   0
                                             :shield   1
                                             :rings    2
                                             :necklace 3
                                             :helm     4
                                             :cloak    5
                                             :chest    6
                                             :leg      7
                                             :glove    8
                                             :boot     9
                                             :bag      10}
        slot->texture-region (fn [slot]
                               (let [width  48
                                     height 48
                                     sprite-x 21
                                     sprite-y (+ (slot->y-sprite-idx slot) 2)
                                     bounds [(* sprite-x width)
                                             (* sprite-y height)
                                             width
                                             height]]
                                 (graphics/texture-region graphics
                                                          {:image/file "images/items.png"
                                                           :image/bounds bounds})))]
    (create-inventory-window*
     {:title "Inventory"
      :actor/visible? false
      :position [(ui/viewport-width  stage)
                 (ui/viewport-height stage)]
      :clicked-cell-listener (fn [cell]
                               (click-listener/create
                                (fn [event x y]
                                  (let [{:keys [ctx/world] :as ctx} (stage/ctx (event/stage event))
                                        eid (:world/player-eid world)
                                        entity @eid
                                        state-k (:state (:entity/fsm entity))
                                        txs (state->clicked-inventory-cell [state-k (state-k entity)]
                                                                           eid
                                                                           cell)]
                                    (txs/handle! ctx txs)))))
      :slot->texture-region slot->texture-region})))

(def state->draw-ui-view
  {:player-item-on-cursor (fn
                            [eid
                             {:keys [ctx/graphics
                                     ctx/input
                                     ctx/stage]}]
                            ; TODO see player-item-on-cursor at render layers
                            ; always draw it here at right position, then render layers does not need input/stage
                            ; can pass world to graphics, not handle here at application
                            (when (not (player-item-on-cursor/world-item? (ui/mouseover-actor stage (input/mouse-position input))))
                              [[:draw/texture-region
                                (graphics/texture-region graphics (:entity/image (:entity/item-on-cursor @eid)))
                                (:graphics/ui-mouse-position graphics)
                                {:center? true}]]))})

(defn- player-state-handle-draws
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [player-eid (:world/player-eid world)
        entity @player-eid
        state-k (:state (:entity/fsm entity))]
    (when-let [f (state->draw-ui-view state-k)]
      (graphics/draw! graphics (f player-eid ctx)))))

(def message-duration-seconds 0.5)

(defn- add-actors! [stage ctx]
  (doseq [actor [(dev-menu/create (create-dev-menu ctx))
                 (action-bar/create)
                 (create-hp-mana-bar* (create-hp-mana-bar ctx))
                 (build-group/create
                  {:actor/name "cdq.ui.windows"
                   :group/actors [(info-window/create (create-entity-info-window ctx))
                                  (create-inventory-window ctx)]})
                 (actor/create
                  {:draw (fn [this _batch _parent-alpha]
                           (player-state-handle-draws (stage/ctx (actor/stage this))))
                   :act (fn [this _delta])})
                 (message/create message-duration-seconds)]]
    (stage/add-actor! stage actor)))

(defn rebuild-actors! [stage ctx]
  (stage/clear! stage)
  (add-actors! stage ctx))

(defn- get-stage-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (or (ui/get-ctx stage)
      ctx)) ; first render stage does not have ctx set.

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input]
    :as ctx}]
  (let [mp (input/mouse-position input)]
    (-> ctx
        (assoc-in [:ctx/graphics :graphics/world-mouse-position] (graphics/unproject-world graphics mp))
        (assoc-in [:ctx/graphics :graphics/ui-mouse-position] (graphics/unproject-ui graphics mp)))))

(defn- update-mouseover-eid!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))
        mouseover-eid (:world/mouseover-eid world)
        new-eid (if mouseover-actor
                  nil
                  (world/mouseover-entity world (:graphics/world-mouse-position graphics)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

(defn- check-open-debug!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/button-just-pressed? input (:open-debug-button input/controls))
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (ui/show-data-viewer! stage data)))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (update ctx :ctx/world world/cache-active-entities))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-position! graphics
                          (:body/position
                           (:entity/body
                            @(:world/player-eid world))))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics color/black)
  ctx)

(defn- tile-color-setter
  [{:keys [ray-blocked?
           explored-tile-corners
           light-position
           see-all-tiles?
           explored-tile-color
           visible-tile-color
           invisible-tile-color]}]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored?
                         explored-tile-color
                         invisible-tile-color)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (ray-blocked? light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles?
            visible-tile-color
            base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              visible-tile-color))))))

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

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter
                             {:ray-blocked? (partial raycaster/blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(defn- draw-image
  [image
   {:keys [entity/body]}
   {:keys [ctx/graphics]}]
  [[:draw/texture-region
    (graphics/texture-region graphics image)
    (:body/position body)
    {:center? true
     :rotation (or (:body/rotation-angle body)
                   0)}]])

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
      [[:draw/filled-rectangle x y width height color/black]
       [:draw/filled-rectangle
        (+ x border)
        (+ y border)
        (- (* width ratio) (* 2 border))
        (- height          (* 2 border))
        (hpbar-color ratio)]])))

(def ^:private skill-image-radius-world-units
  (let [tile-size 48
        image-width 32]
    (/ (/ image-width tile-size) 2)))

(defn- draw-skill-image
  [texture-region entity [x y] action-counter-ratio]
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
     [:draw/texture-region texture-region [(- (float x) radius) y]]]))

(def effect-k->fn
  {:effects/target-all {:draw (fn [_
                                   {:keys [effect/source]}
                                   {:keys [ctx/world]}]
                                (let [{:keys [world/active-entities]} world
                                      source* @source]
                                  (for [target* (map deref (target-all/affected-targets active-entities world source*))]
                                    [:draw/line
                                     (:body/position (:entity/body source*)) #_(start-point source* target*)
                                     (:body/position (:entity/body target*))
                                     [1 0 0 0.5]])))}

   :effects/target-entity {:draw (fn [[_ {:keys [maxrange]}]
                                      {:keys [effect/source effect/target]}
                                      _ctx]
                                   (when target
                                     (let [body        (:entity/body @source)
                                           target-body (:entity/body @target)]
                                       [[:draw/line
                                         (target-entity/start-point body target-body)
                                         (target-entity/end-point body target-body maxrange)
                                         (if (target-entity/in-range? body target-body maxrange)
                                           [1 0 0 0.5]
                                           [1 1 0 0.5])]])))}})

(defn draw-effect [{k 0 :as component} effect-ctx ctx]
  (if-let [f (:draw (effect-k->fn k))]
    (f component effect-ctx ctx)
    nil))

(def ^:private render-layers
  (let [outline-alpha 0.4
        enemy-color [1 0 0 outline-alpha]
        friendly-color [0 1 0 outline-alpha]
        neutral-color [1 1 1 outline-alpha]
        mouseover-ellipse-width 5]
    [{:entity/mouseover?     (fn
                               [_
                                {:keys [entity/body
                                        entity/faction]}
                                {:keys [ctx/world]}]
                               (let [player @(:world/player-eid world)]
                                 [[:draw/with-line-width mouseover-ellipse-width
                                   [[:draw/ellipse
                                     (:body/position body)
                                     (/ (:body/width  body) 2)
                                     (/ (:body/height body) 2)
                                     (cond (= faction (faction/enemy (:entity/faction player)))
                                           enemy-color
                                           (= faction (:entity/faction player))
                                           friendly-color
                                           :else
                                           neutral-color)]]]]))

      :stunned               (fn [_ {:keys [entity/body]} _ctx]
                               [[:draw/circle
                                 (:body/position body)
                                 0.5
                                 [1 1 1 0.6]]])

      :player-item-on-cursor (fn
                               [{:keys [item]}
                                entity
                                {:keys [ctx/graphics
                                        ctx/input
                                        ctx/stage]}]
                               ; TODO do not draw here, only at UI view
                               ; then graphics can draw world without stage/input
                               (when (player-item-on-cursor/world-item? (ui/mouseover-actor stage (input/mouse-position input)))
                                 [[:draw/texture-region
                                   (graphics/texture-region graphics (:entity/image item))
                                   (player-item-on-cursor/item-place-position (:graphics/world-mouse-position graphics)
                                                                              entity)
                                   {:center? true}]]))}
     {:entity/clickable      (fn
                               [{:keys [text]}
                                {:keys [entity/body
                                        entity/mouseover?]}
                                _ctx]
                               (when (and mouseover? text)
                                 (let [[x y] (:body/position body)]
                                   [[:draw/text {:text text
                                                 :x x
                                                 :y (+ y (/ (:body/height body) 2))
                                                 :up? true}]])))

      :entity/animation      (fn [animation entity ctx]
                               (draw-image (animation/current-frame animation)
                                           entity
                                           ctx))

      :entity/image          draw-image

      :entity/line-render    (fn [{:keys [thick? end color]} {:keys [entity/body]} _ctx]
                               (let [position (:body/position body)]
                                 (if thick?
                                   [[:draw/with-line-width
                                     4
                                     [[:draw/line position end color]]]]
                                   [[:draw/line position end color]])))}

     {:npc-sleeping          (fn [_ {:keys [entity/body]} _ctx]
                               (let [[x y] (:body/position body)]
                                 [[:draw/text {:text "zzz"
                                               :x x
                                               :y (+ y (/ (:body/height body) 2))
                                               :up? true}]]))

      :entity/temp-modifier  (fn [_ entity _ctx]
                               [[:draw/filled-circle
                                 (:body/position (:entity/body entity))
                                 0.5
                                 [0.5 0.5 0.5 0.4]]])

      :entity/string-effect  (fn [{:keys [text]} entity {:keys [ctx/graphics]}]
                               (let [[x y] (:body/position (:entity/body entity))]
                                 [[:draw/text {:text text
                                               :x x
                                               :y (+ y
                                                     (/ (:body/height (:entity/body entity)) 2)
                                                     (* 5 (:graphics/world-unit-scale graphics)))
                                               :scale 2
                                               :up? true}]]))}

     {:entity/stats          (fn [_ entity {:keys [ctx/graphics]}]
                               (let [ratio (val-max/ratio (stats/get-hitpoints (:entity/stats entity)))]
                                 (when (or (< ratio 1) (:entity/mouseover? entity))
                                   (draw-hpbar (:graphics/world-unit-scale graphics)
                                               (:entity/body entity)
                                               ratio))))
      :active-skill          (fn
                               [{:keys [skill effect-ctx counter]}
                                entity
                                {:keys [ctx/graphics
                                        ctx/world]
                                 :as ctx}]
                               (let [{:keys [entity/image skill/effects]} skill]
                                 (concat (draw-skill-image (graphics/texture-region graphics image)
                                                           entity
                                                           (:body/position (:entity/body entity))
                                                           (timer/ratio (:world/elapsed-time world) counter))
                                         (mapcat #(draw-effect % effect-ctx ctx)  ; update-effect-ctx here too ?
                                                 effects))))}]))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity
  [{:keys [ctx/graphics]
    :as ctx}
   entity render-layer]
  (try (do
        (when show-body-bounds?
          (graphics/draw! graphics (draw-body-rect (:entity/body entity)
                                                   (if (:body/collides? (:entity/body entity))
                                                     color/white
                                                     color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/draw! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/draw! graphics (draw-body-rect (:entity/body entity) color/red))
         (throwable/pretty-pst t))))

(defn draw-entities
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (raycaster/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(def ^:dbg-flag show-tile-grid? false)

(defn draw-tile-grid
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (graphics/frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (graphics/world-vp-width  graphics)))
        (+ 2 (int (graphics/world-vp-height graphics)))
        1
        1
        [1 1 1 0.8]]])))

(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn draw-cell-debug
  [{:keys [ctx/graphics
           ctx/world]}]
  (apply concat
         (for [[x y] (graphics/visible-tiles graphics)
               :let [cell ((:world/grid world) [x y])]
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
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(comment
 (require '[cdq.world.grid :as grid])
 (require '[clojure.math.geom :as geom])

 (defn geom-test
   [{:keys [ctx/graphics
            ctx/world]}]
   (let [position (:graphics/world-mouse-position graphics)
         radius 0.8
         circle {:position position
                 :radius radius}]
     (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                 (for [[x y] (map #(:position @%) (grid/circle->cells (:world/grid world) circle))]
                   [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
           (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
             [:draw/rectangle x y width height [0 0 1 1]]))))

 )

(defn highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [[x y] (mapv int (:graphics/world-mouse-position graphics))
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx} ]
  (graphics/draw-on-world-vp! graphics
                              (fn []
                                (doseq [f [draw-tile-grid
                                           draw-cell-debug
                                           draw-entities
                                           #_geom-test
                                           highlight-mouseover-tile]]
                                  (graphics/draw! graphics (f ctx)))))
  ctx)

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (:body/position (:entity/body @mouseover-eid)))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (:body/position (:entity/body @player-eid))
                                           target-position)}))

(defn- interaction-state
  [stage
   world-mouse-position
   mouseover-eid
   player-eid
   mouseover-actor]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor (ui/actor-information mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (body/distance (:entity/body @player-eid)
                                        (:entity/body @mouseover-eid))
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (ui/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (skill/usable-state skill entity effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn- assoc-interaction-state
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (assoc ctx :ctx/interaction-state (interaction-state stage
                                                       (:graphics/world-mouse-position graphics)
                                                       (:world/mouseover-eid world)
                                                       (:world/player-eid    world)
                                                       (ui/mouseover-actor stage (input/mouse-position input)))))

(defn- player-idle->cursor [player-eid {:keys [ctx/interaction-state]}]
  (let [[k params] interaction-state]
    (case k
      :interaction-state/mouseover-actor
      (let [[actor-type params] params
            inventory-cell-with-item? (and (= actor-type :mouseover-actor/inventory-cell)
                                           (let [inventory-slot params]
                                             (get-in (:entity/inventory @player-eid) inventory-slot)))]
        (cond
          inventory-cell-with-item?
          :cursors/hand-before-grab

          (= actor-type :mouseover-actor/window-title-bar)
          :cursors/move-window

          (= actor-type :mouseover-actor/button)
          :cursors/over-button

          (= actor-type :mouseover-actor/unspecified)
          :cursors/default

          :else
          :cursors/default))

      :interaction-state/clickable-mouseover-eid
      (let [{:keys [clicked-eid
                    in-click-range?]} params]
        (case (:type (:entity/clickable @clicked-eid))
          :clickable/item (if in-click-range?
                            :cursors/hand-before-grab
                            :cursors/hand-before-grab-gray)
          :clickable/player :cursors/bag))

      :interaction-state.skill/usable
      :cursors/use-skill

      :interaction-state.skill/not-usable
      :cursors/skill-not-usable

      :interaction-state/no-skill-selected
      :cursors/no-skill-selected)))

(let [fn-map {:active-skill :cursors/sandclock
              :player-dead :cursors/black-x
              :player-idle player-idle->cursor
              :player-item-on-cursor :cursors/hand-grab
              :player-moving :cursors/walking
              :stunned :cursors/denied}]
  (defn state->cursor [[k v] eid ctx]
    (let [->cursor (k fn-map)]
      (if (keyword? ->cursor)
        ->cursor
        (->cursor eid ctx)))))

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state->cursor [state-k (state-k entity)] eid ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn- interaction-state->txs [[k params] stage player-eid]
  (case k
    :interaction-state/mouseover-actor nil

    :interaction-state/clickable-mouseover-eid
    (let [{:keys [clicked-eid
                  in-click-range?]} params]
      (if in-click-range?
        (case (:type (:entity/clickable @clicked-eid))
          :clickable/player
          [[:tx/toggle-inventory-visible]]

          :clickable/item
          (let [item (:entity/item @clicked-eid)]
            (cond
             (ui/inventory-window-visible? stage)
             [[:tx/sound "bfxr_takeit"]
              [:tx/mark-destroyed clicked-eid]
              [:tx/event player-eid :pickup-item item]]

             (inventory/can-pickup-item? (:entity/inventory @player-eid) item)
             [[:tx/sound "bfxr_pickup"]
              [:tx/mark-destroyed clicked-eid]
              [:tx/pickup-item player-eid item]]

             :else
             [[:tx/sound "bfxr_denied"]
              [:tx/show-message "Your Inventory is full"]])))
        [[:tx/sound "bfxr_denied"]
         [:tx/show-message "Too far away"]]))

    :interaction-state.skill/usable
    (let [[skill effect-ctx] params]
      [[:tx/event player-eid :start-action [skill effect-ctx]]])

    :interaction-state.skill/not-usable
    (let [state params]
      [[:tx/sound "bfxr_denied"]
       [:tx/show-message (case state
                           :cooldown "Skill is still on cooldown"
                           :not-enough-mana "Not enough mana"
                           :invalid-params "Cannot use this here")]])

    :interaction-state/no-skill-selected
    [[:tx/sound "bfxr_denied"]
     [:tx/show-message "No selected skill"]]))

(let [fn-map {:player-idle           (fn
                                       [player-eid
                                        {:keys [ctx/input
                                                ctx/interaction-state
                                                ctx/stage] :as ctx}]
                                       (if-let [movement-vector (input/player-movement-vector input)]
                                         [[:tx/event player-eid :movement-input movement-vector]]
                                         (when (input/button-just-pressed? input input.buttons/left)
                                           (interaction-state->txs interaction-state
                                                                   stage
                                                                   player-eid))))

              :player-item-on-cursor (fn
                                       [eid {:keys [ctx/input
                                                    ctx/stage]}]
                                       (let [mouseover-actor (ui/mouseover-actor stage (input/mouse-position input))]
                                         (when (and (input/button-just-pressed? input input.buttons/left)
                                                    (player-item-on-cursor/world-item? mouseover-actor))
                                           [[:tx/event eid :drop-item]])))

              :player-moving         (let [speed (fn [{:keys [entity/stats]}]
                                                   (or (stats/get-stat-value stats :stats/movement-speed)
                                                       0))]
                                       (fn [eid {:keys [ctx/input]}]
                                         (if-let [movement-vector (input/player-movement-vector input)]
                                           [[:tx/assoc eid :entity/movement {:direction movement-vector
                                                                             :speed (speed @eid)}]]
                                           [[:tx/event eid :no-movement-input]])))}]
  (defn state->handle-input [[k v] eid ctx]
    (if-let [f (k fn-map)]
      (f eid ctx)
      nil)))

(defn- player-state-handle-input!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state->handle-input [state-k (state-k entity)] eid ctx)]
    (txs/handle! ctx txs))
  ctx)

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (or (input/key-just-pressed? input (:unpause-once input/controls))
                              (input/key-pressed? input (:unpause-continously input/controls))))))))

(defn- update-world-time* [{:keys [world/max-delta]
                           :as world}
                          delta-ms]
  (let [delta-ms (min delta-ms max-delta)]
    (-> world
        (assoc :world/delta-time delta-ms)
        (update :world/elapsed-time + delta-ms))))

(defn- update-world-time
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (if (:world/paused? (:ctx/world ctx))
    ctx
    (update ctx :ctx/world update-world-time* (graphics/delta-time graphics))))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do
     (world/update-potential-fields! world)
     ctx)))

(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (txs/handle! ctx (world/tick-entities! world))
         (catch Throwable t
           (throwable/pretty-pst t)
           (ui/show-error-window! stage t)))
        ctx)))

(defn- remove-destroyed-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (txs/handle! ctx (world/remove-destroyed-entities! world))
  ctx)

(def zoom-speed 0.025)

(defn- window-camera-controls!
  [{:keys [ctx/input
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (when (input/key-pressed? input (:zoom-in input/controls))
    (graphics/change-zoom! graphics zoom-speed))

  (when (input/key-pressed? input (:zoom-out input/controls))
    (graphics/change-zoom! graphics (- zoom-speed)))

  (when (input/key-just-pressed? input (:close-windows-key input/controls))
    (ui/close-all-windows! stage))

  (when (input/key-just-pressed? input (:toggle-inventory input/controls))
    (ui/toggle-inventory-visible! stage))

  (when (input/key-just-pressed? input (:toggle-entity-info input/controls))
    (ui/toggle-entity-info-window! stage))
  ctx)

(defn- act-draw-stage!
  [{:keys [^Stage ctx/stage]
    :as ctx}]
  (set! (.ctx stage) ctx)
  (.act  stage)
  (.draw stage)
  (.ctx  stage))

(def reaction-txs-fn-map
  {
   :tx/set-item    (fn
                     [{:keys [ctx/graphics
                              ctx/stage]}
                      eid cell item]
                     (when (:entity/player? @eid)
                       (ui/set-item! stage cell
                                     {:texture-region (graphics/texture-region graphics (:entity/image item))
                                      :tooltip-text (info/text item nil)})
                       nil))

   :tx/remove-item (fn
                     [{:keys [ctx/stage]}
                      eid cell]
                     (when (:entity/player? @eid)
                       (ui/remove-item! stage cell)
                       nil))

   :tx/add-skill   (fn
                     [{:keys [ctx/graphics
                              ctx/stage]}
                      eid skill]
                     (when (:entity/player? @eid)
                       (ui/add-skill! stage
                                      {:skill-id (:property/id skill)
                                       :texture-region (graphics/texture-region graphics (:entity/image skill))
                                       :tooltip-text (fn [{:keys [ctx/world]}]
                                                       (info/text skill world))})
                       nil))
   }
  )

(require '[cdq.entity.skills :as skills])
(require '[clojure.timer :as timer])
(require '[cdq.entity.inventory :as inventory]
         '[cdq.entity.stats :as stats])

(require '[cdq.entity.state :as state]
         '[reduce-fsm :as fsm]
         '[cdq.effect :as effect]
         )

(require '[cdq.world.content-grid :as content-grid]
         '[cdq.world.grid :as grid]
         '[clojure.math.vector2 :as v])

(defn world-move-entity
  [{:keys [world/content-grid
           world/grid]}
   eid body direction rotate-in-movement-direction?]
  (content-grid/position-changed! content-grid eid)
  (grid/remove-from-touched-cells! grid eid)
  (grid/set-touched-cells! grid eid)
  (when (:body/collides? (:entity/body @eid))
    (grid/remove-from-occupied-cells! grid eid)
    (grid/set-occupied-cells! grid eid))
  (swap! eid assoc-in [:entity/body :body/position] (:body/position body))
  (when rotate-in-movement-direction?
    (swap! eid assoc-in [:entity/body :body/rotation-angle] (v/angle-from-vector direction)))
  nil)

(defn world-handle-event
  ([world eid event]
   (world-handle-event world eid event nil))
  ([world eid event params]
   (let [fsm (:entity/fsm @eid)
         _ (assert fsm)
         old-state-k (:state fsm)
         new-fsm (fsm/fsm-event fsm event)
         new-state-k (:state new-fsm)]
     (when-not (= old-state-k new-state-k)
       (let [old-state-obj (let [k (:state (:entity/fsm @eid))]
                             [k (k @eid)])
             new-state-obj [new-state-k (state/create [new-state-k params] eid world)]]
         [[:tx/assoc       eid :entity/fsm new-fsm]
          [:tx/assoc       eid new-state-k (new-state-obj 1)]
          [:tx/dissoc      eid old-state-k]
          [:tx/state-exit  eid old-state-obj]
          [:tx/state-enter eid new-state-obj]])))))

(def txs-fn-map
  {
   :tx/assoc                    (fn [_ctx eid k value]
                                  (swap! eid assoc k value)
                                  nil)

   :tx/assoc-in                 (fn [_ctx eid ks value]
                                  (swap! eid assoc-in ks value)
                                  nil)

   :tx/dissoc                   (fn [_ctx eid k]
                                  (swap! eid dissoc k)
                                  nil)

   :tx/update                   (fn [_ctx eid & params]
                                  (apply swap! eid update params)
                                  nil)

   :tx/mark-destroyed           (fn [_ctx eid]
                                  (swap! eid assoc :entity/destroyed? true)
                                  nil)

   :tx/set-cooldown             (fn [{:keys [ctx/world]} eid skill]
                                  (swap! eid update :entity/skills skills/set-cooldown skill (:world/elapsed-time world))
                                  nil)

   :tx/add-text-effect          (fn [{:keys [ctx/world]} eid text duration]
                                  [[:tx/assoc
                                    eid
                                    :entity/string-effect
                                    (if-let [existing (:entity/string-effect @eid)]
                                      (-> existing
                                          (update :text str "\n" text)
                                          (update :counter timer/increment duration))
                                      {:text text
                                       :counter (timer/create (:world/elapsed-time world) duration)})]])

   :tx/add-skill                (fn [_ctx eid {:keys [property/id] :as skill}]
                                  {:pre [(not (contains? (:entity/skills @eid) id))]}
                                  (swap! eid update :entity/skills assoc id skill)
                                  nil)

   :tx/set-item                 (fn [_ctx eid cell item]
                                  (let [entity @eid
                                        inventory (:entity/inventory entity)]
                                    (assert (and (nil? (get-in inventory cell))
                                                 (inventory/valid-slot? cell item)))
                                    (swap! eid assoc-in (cons :entity/inventory cell) item)
                                    (when (inventory/applies-modifiers? cell)
                                      (swap! eid update :entity/stats stats/add (:stats/modifiers item)))
                                    nil))

   :tx/remove-item              (fn [_ctx eid cell]
                                  (let [entity @eid
                                        item (get-in (:entity/inventory entity) cell)]
                                    (assert item)
                                    (swap! eid assoc-in (cons :entity/inventory cell) nil)
                                    (when (inventory/applies-modifiers? cell)
                                      (swap! eid update :entity/stats stats/remove-mods (:stats/modifiers item)))
                                    nil))

   :tx/pickup-item              (fn [_ctx eid item]
                                  (inventory/assert-valid-item? item)
                                  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
                                    (assert cell)
                                    (assert (or (inventory/stackable? item cell-item)
                                                (nil? cell-item)))
                                    (if (inventory/stackable? item cell-item)
                                      (do
                                       #_(tx/stack-item ctx eid cell item))
                                      [[:tx/set-item eid cell item]])))

   :tx/event                    (fn [{:keys [ctx/world]} & params]
                                  (apply world-handle-event world params))

   :tx/state-exit               (fn [ctx eid [state-k state-v]]
                                  (state/exit [state-k state-v] eid ctx))

   :tx/state-enter              (fn [_ctx eid [state-k state-v]]
                                  (state/enter [state-k state-v] eid))

   :tx/effect                   (fn [{:keys [ctx/world]} effect-ctx effects]
                                  (mapcat #(effect/handle % effect-ctx world)
                                          (filter #(effect/applicable? % effect-ctx) effects)))

   :tx/audiovisual              (fn
                                  [{:keys [ctx/db]} position audiovisual]
                                  (let [{:keys [tx/sound
                                                entity/animation]} (if (keyword? audiovisual)
                                                                     (db/build db audiovisual)
                                                                     audiovisual)]
                                    [[:tx/sound sound]
                                     [:tx/spawn-effect
                                      position
                                      {:entity/animation (assoc animation :delete-after-stopped? true)}]]))

   :tx/spawn-alert              (fn [{:keys [ctx/world]} position faction duration]
                                  [[:tx/spawn-effect
                                    position
                                    {:entity/alert-friendlies-after-duration
                                     {:counter (timer/create (:world/elapsed-time world) duration)
                                      :faction faction}}]])

   :tx/spawn-line               (fn [_ctx {:keys [start end duration color thick?]}]
                                  [[:tx/spawn-effect
                                    start
                                    {:entity/line-render {:thick? thick? :end end :color color}
                                     :entity/delete-after-duration duration}]])

   :tx/move-entity              (fn [{:keys [ctx/world]} & params]
                                  (apply world-move-entity world params))

   :tx/spawn-projectile         (fn
                                  [_ctx
                                   {:keys [position direction faction]}
                                   {:keys [entity/image
                                           projectile/max-range
                                           projectile/speed
                                           entity-effects
                                           projectile/size
                                           projectile/piercing?] :as projectile}]
                                  [[:tx/spawn-entity
                                    {:entity/body {:position position
                                                   :width size
                                                   :height size
                                                   :z-order :z-order/flying
                                                   :rotation-angle (v/angle-from-vector direction)}
                                     :entity/movement {:direction direction
                                                       :speed speed}
                                     :entity/image image
                                     :entity/faction faction
                                     :entity/delete-after-duration (/ max-range speed)
                                     :entity/destroy-audiovisual :audiovisuals/hit-wall
                                     :entity/projectile-collision {:entity-effects entity-effects
                                                                   :piercing? piercing?}}]])

   :tx/spawn-effect             (fn
                                  [{:keys [ctx/world]}
                                   position
                                   components]
                                  [[:tx/spawn-entity
                                    (assoc components
                                           :entity/body (assoc (:world/effect-body-props world) :position position))]])

   :tx/spawn-item               (fn [_ctx position item]
                                  [[:tx/spawn-entity
                                    {:entity/body {:position position
                                                   :width 0.75
                                                   :height 0.75
                                                   :z-order :z-order/on-ground}
                                     :entity/image (:entity/image item)
                                     :entity/item item
                                     :entity/clickable {:type :clickable/item
                                                        :text (:property/pretty-name item)}}]])

   :tx/spawn-creature           (fn
                                  [_ctx
                                   {:keys [position
                                           creature-property
                                           components]}]
                                  (assert creature-property)
                                  [[:tx/spawn-entity
                                    (-> creature-property
                                        (assoc :entity/body (let [{:keys [body/width body/height #_body/flying?]} (:entity/body creature-property)]
                                                              {:position position
                                                               :width  width
                                                               :height height
                                                               :collides? true
                                                               :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)}))
                                        (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                                        (utils/safe-merge components))]])

   :tx/spawn-entity             (fn [{:keys [ctx/world]} entity]
                                  (world/spawn-entity! world entity))

   :tx/sound                    (fn [{:keys [ctx/audio]} sound-name]
                                  (audio/play! audio sound-name)
                                  nil)
   :tx/toggle-inventory-visible (fn [{:keys [ctx/stage]}]
                                  (ui/toggle-inventory-visible! stage)
                                  nil)
   :tx/show-message             (fn [{:keys [ctx/stage]} message]
                                  (ui/show-text-message! stage message)
                                  nil)
   :tx/show-modal               (fn [{:keys [ctx/stage]} opts]
                                  (ui/show-modal-window! stage (clojure.gdx.scene2d.stage/viewport stage) opts)
                                  nil)
   }
  )

(extend-type Context
  txs/TransactionHandler
  (handle! [ctx txs]
    (let [handled-txs (tx-handler/actions! txs-fn-map ctx txs)]
      (tx-handler/actions! reaction-txs-fn-map
                           ctx
                           handled-txs
                           :strict? false))))

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle! ctx
               [[:tx/spawn-creature (let [{:keys [creature-id
                                                  components]} (:world/player-components world)]
                                      {:position (mapv (partial + 0.5) (:world/start-position world))
                                       :creature-property (db/build db creature-id)
                                       :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle!
   ctx
   (for [[position creature-id] (tiled-map/spawn-positions (:world/tiled-map world))]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(graphics/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(def ^:private world-params
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
   :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                         :initial-state :npc-sleeping}
                            :entity/faction :evil}
   :world/player-components {:creature-id :creatures/vampire
                             :components {:entity/fsm {:fsm :fsms/player
                                                       :initial-state :player-idle}
                                          :entity/faction :good
                                          :entity/player? true
                                          :entity/free-skill-points 3
                                          :entity/clickable {:type :clickable/player}
                                          :entity/click-distance-tiles 1.5}}
   :world/effect-body-props {:width 0.5
                             :height 0.5
                             :z-order :z-order/effect}})

(defn- create-world
  [{:keys [ctx/db
           ctx/graphics
           ctx/world]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (-> ctx
        (assoc :ctx/world (world/create world-params world-fn-result))
        spawn-player!
        spawn-enemies!)))

(defn create!
  [{:keys [audio
           files
           graphics
           input]}
   config]
  (let [graphics (graphics/create! graphics files (:graphics config))
        stage (ui/create! graphics)
        ctx (map->Context {})
        ctx (merge ctx
                   {:ctx/audio (audio/create audio files (:audio config))
                    :ctx/db (db/create)
                    :ctx/graphics graphics
                    :ctx/input input
                    :ctx/stage stage})]
    (input/set-processor! input stage)
    (add-actors! stage ctx)
    (create-world ctx (:world config))))

(defn dispose!
  [{:keys [ctx/audio
           ctx/graphics
           ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (ui/dispose!)
  (world/dispose! world))

(defn render! [ctx]
  (-> ctx
      get-stage-ctx
      validate
      update-mouse
      update-mouseover-eid!
      check-open-debug!
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      assoc-interaction-state
      set-cursor!
      player-state-handle-input!
      (dissoc :ctx/interaction-state)
      assoc-paused
      update-world-time
      update-potential-fields!
      tick-entities!
      remove-destroyed-entities!
      window-camera-controls!
      act-draw-stage!
      validate))

(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/update-ui-viewport! graphics width height)
  (graphics/update-world-vp! graphics width height))
