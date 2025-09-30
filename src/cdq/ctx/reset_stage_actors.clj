(ns cdq.ctx.reset-stage-actors
  (:require [cdq.ctx.handle-txs :as handle-txs]
            [cdq.db :as db]
            [cdq.entity.state :as state]
            [cdq.entity.stats :as stats]
            [cdq.entity.inventory :as inventory]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.ui.action-bar]
            [cdq.ui.inventory]
            [cdq.val-max :as val-max]
            [clojure.string :as str]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.image :as image]
            [com.badlogic.gdx.scenes.scene2d.ui.label :as label]
            [com.badlogic.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [com.badlogic.gdx.scenes.scene2d.utils.drawable :as drawable]
            [com.badlogic.gdx.scenes.scene2d.utils.listener :as listener]
            [gdl.utils :as utils]))

(let [open-editor (fn [db]
                    {:label "Editor"
                     :items (for [property-type (sort (db/property-types db))]
                              {:label (str/capitalize (name property-type))
                               :on-click (fn [_actor {:keys [ctx/db
                                                             ctx/graphics
                                                             ctx/stage]}]
                                           (stage/add!
                                            stage
                                            (scene2d/build
                                             {:actor/type :actor.type/editor-overview-window
                                              :db db
                                              :graphics graphics
                                              :property-type property-type
                                              :clicked-id-fn (fn [_actor id {:keys [ctx/stage] :as ctx}]
                                                               (stage/add! stage
                                                                           (scene2d/build
                                                                            {:actor/type :actor.type/editor-window
                                                                             :ctx ctx
                                                                             :property (db/get-raw db id)})))})))})})
      ctx-data-viewer {:label "Ctx Data"
                       :items [{:label "Show data"
                                :on-click (fn [_actor {:keys [ctx/stage] :as ctx}]
                                            (stage/add! stage (scene2d/build
                                                               {:actor/type :actor.type/data-viewer
                                                                :title "Context"
                                                                :data ctx
                                                                :width 500
                                                                :height 500})))}]}
      help-str "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"
      help-info-text {:label "Help"
                      :items [{:label help-str}]}
      select-world {:label "Select World"
                    :items (for [world-fn ["world_fns/vampire.edn"
                                           "world_fns/uf_caves.edn"
                                           "world_fns/modules.edn"]]
                             {:label (str "Start " world-fn)
                              :on-click (fn [actor ctx]
                                          (stage/set-ctx! (actor/get-stage actor)
                                                          ((requiring-resolve 'cdq.ctx.reset-game-state/do!) ctx world-fn)))})}
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
                                   (graphics/camera-zoom (:ctx/graphics ctx)))
                      :icon "images/zoom.png"}]]
  (defn- create-dev-menu
    [db graphics]
    {:actor/type :actor.type/table
     :rows [[{:actor {:actor/type :actor.type/menu-bar
                      :menus [ctx-data-viewer
                              (open-editor db)
                              help-info-text
                              select-world]
                      :update-labels (for [item update-labels]
                                       (if (:icon item)
                                         (update item :icon #(get (:graphics/textures graphics) %))
                                         item))}
              :expand-x? true
              :fill-x? true
              :colspan 1}]
            [{:actor {:actor/type :actor.type/label
                      :label/text ""
                      :actor/touchable :disabled}
              :expand? true
              :fill-x? true
              :fill-y? true}]]
     :fill-parent? true}))

(defn- create-inventory-window*
  [stage
   {:keys [title
           actor/visible?
           clicked-cell-listener
           slot->texture-region]}]
  (let [cell-size 48
        slot->drawable (fn [slot]
                         (drawable/create (slot->texture-region slot)
                                         :width cell-size
                                         :height cell-size
                                         :tint-color [1 1 1 0.4]))
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
        draw-rect-actor (fn []
                          {:actor/type :actor.type/widget
                           :draw (fn [actor {:keys [ctx/graphics
                                                    ctx/world]}]
                                   (draw-cell-rect @(:world/player-eid world)
                                                   (actor/get-x actor)
                                                   (actor/get-y actor)
                                                   (actor/hit actor
                                                              (actor/stage->local-coordinates actor (:graphics/ui-mouse-position graphics)))
                                                   (actor/user-object (actor/parent actor))))})
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :actor/name "inventory-cell"
                            :actor/user-object cell
                            :actor/listener (clicked-cell-listener cell)
                            :group/actors [(draw-rect-actor)
                                           {:actor/type :actor.type/image
                                            :image/object background-drawable
                                            :actor/name "image-widget"
                                            :actor/user-object {:background-drawable background-drawable
                                                                :cell-size cell-size}}]}}))]
    (scene2d/build
     {:actor/type :actor.type/window
      :title title
      :actor/name "cdq.ui.windows.inventory"
      :actor/visible? visible?
      :pack? true
      :actor/position [(:viewport/width  (stage/viewport stage))
                       (:viewport/height (stage/viewport stage))]
      :rows [[{:actor {:actor/name "inventory-cell-table"
                       :actor/type :actor.type/table
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
                                         (->cell :inventory.slot/bag :position [x y]))))}
               :pad 4}]]})))

(defn- find-cell [group cell]
  (first (filter #(= (actor/user-object % ) cell)
                 (group/children group))))

(defn- window->cell [inventory-window cell]
  (-> inventory-window
      (group/find-actor "inventory-cell-table")
      (find-cell cell)))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Window
  cdq.ui.inventory/Inventory
  (set-item! [inventory-window cell {:keys [texture-region tooltip-text]}]
    (let [cell-widget (window->cell inventory-window cell)
          image-widget (group/find-actor cell-widget "image-widget")
          cell-size (:cell-size (actor/user-object image-widget))
          drawable (drawable/create texture-region :width cell-size :height cell-size)]
      (image/set-drawable! image-widget drawable)
      (actor/add-tooltip! cell-widget tooltip-text)))

  (remove-item! [inventory-window cell]
    (let [cell-widget (window->cell inventory-window cell)
          image-widget (group/find-actor cell-widget "image-widget")]
      (image/set-drawable! image-widget (:background-drawable (actor/user-object image-widget)))
      (actor/remove-tooltip! cell-widget)))

  (cell-with-item? [_ actor]
    (and (actor/parent actor)
         (= "inventory-cell" (actor/get-name (actor/parent actor)))
         (actor/user-object (actor/parent actor)))))

(defn- create-entity-info-window [stage]
  (let [title "info"
        actor-name "cdq.ui.windows.entity-info"
        visible? false
        position [(cdq.stage/viewport-width stage) 0]
        set-label-text! (fn [{:keys [ctx/world]}]
                          (if-let [eid (:world/mouseover-eid world)]
                            (info/info-text (apply dissoc @eid [:entity/skills
                                                                :entity/faction
                                                                :active-skill])
                                            world)
                            ""))
        label (scene2d/build {:actor/type :actor.type/label
                              :label/text ""})
        window (scene2d/build {:actor/type :actor.type/window
                               :title title
                               :actor/name actor-name
                               :actor/visible? visible?
                               :actor/position position
                               :rows [[{:actor label :expand? true}]]})]
    (group/add! window (scene2d/build
                        {:actor/type :actor.type/actor
                         :act (fn [_this _delta ctx]
                                (label/set-text! label (str (set-label-text! ctx)))
                                (widget-group/pack! window))}))
    window))

(defn- create-inventory-window
  [stage graphics]
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
     stage
     {:title "Inventory"
      :actor/visible? false
      :clicked-cell-listener (fn [cell]
                               (listener/click
                                (fn [event _x _y]
                                  (let [{:keys [ctx/world] :as ctx} (stage/get-ctx (event/stage event))
                                        eid (:world/player-eid world)
                                        entity @eid
                                        state-k (:state (:entity/fsm entity))
                                        txs (state/clicked-inventory-cell [state-k (state-k entity)]
                                                                          eid
                                                                          cell)]
                                    (handle-txs/do! ctx txs)))))
      :slot->texture-region slot->texture-region})))

(defn- create-ui-windows [stage graphics]
  {:actor/type :actor.type/group
   :actor/name "cdq.ui.windows"
   :group/actors [(create-entity-info-window stage)
                  (create-inventory-window stage graphics)]})

(let [config {:rahmen-file "images/rahmen.png"
              :rahmenw 150
              :rahmenh 26
              :hpcontent-file "images/hp.png"
              :manacontent-file "images/mana.png"
              :y-mana 80}]
  (defn- create-hp-mana-bar
    [stage graphics]
    (let [{:keys [rahmen-file
                  rahmenw
                  rahmenh
                  hpcontent-file
                  manacontent-file
                  y-mana]} config
          [x y-mana] [(/ (cdq.stage/viewport-width stage) 2)
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
                                            :up? true}]])
          create-draws (fn [{:keys [ctx/world]}]
                         (let [stats (:entity/stats @(:world/player-eid world))
                               x (- x (/ rahmenw 2))]
                           (concat
                            (render-hpmana-bar x y-hp   hpcontent-file   (stats/get-hitpoints stats) "HP")
                            (render-hpmana-bar x y-mana manacontent-file (stats/get-mana      stats) "MP"))))]
      {:actor/type :actor.type/actor
       :draw (fn [_this ctx]
               (create-draws ctx))})))

(defn- create-player-state-draw-ui []
  {:actor/type :actor.type/actor
   :draw (fn [_this {:keys [ctx/world]
                     :as ctx}]
           (let [player-eid (:world/player-eid world)
                 entity @player-eid
                 state-k (:state (:entity/fsm entity))]
             (state/draw-gui-view [state-k (state-k entity)]
                                  player-eid
                                  ctx)))})

(defn do!
  [{:keys [ctx/db
           ctx/graphics
           ctx/stage]
    :as ctx}]
  (stage/clear! stage)
  (let [actors [(create-dev-menu db graphics)
                (cdq.ui.action-bar/create)
                (create-hp-mana-bar stage graphics)
                (create-ui-windows stage graphics)
                (create-player-state-draw-ui)
                (cdq.ui.message/create)]]
    (doseq [actor actors]
      (stage/add! stage (scene2d/build actor))))
  ctx)
