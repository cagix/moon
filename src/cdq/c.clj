; TODO REMOVE REQUIRING-RESOLVE
(ns cdq.c
  (:require cdq.application.create.editor
            cdq.ui.editor.window
            cdq.application.create.stage
            cdq.application.render.update-potential-fields
            cdq.application.create.input
            cdq.application.create.db
            [cdq.application.create.world]
            [clj-commons.pretty.repl :as pretty-repl]
            [clojure.edn :as edn]
            [clojure.graphics.color :as color]
            [clojure.scene2d.vis-ui :as vis-ui]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cdq.audio :as audio]
            [cdq.creature :as creature]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity.body :as body]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.malli :as m]
            [cdq.stage]
            [cdq.stats :as modifiers]
            [cdq.ui.action-bar]
            [cdq.ui.inventory]
            [cdq.val-max :as val-max]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.grid :as grid]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.ctx]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.event :as event]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.scenes.scene2d.ui.image :as image]
            [com.badlogic.gdx.scenes.scene2d.ui.label :as label]
            [com.badlogic.gdx.scenes.scene2d.ui.widget-group :as widget-group]
            [com.badlogic.gdx.scenes.scene2d.utils.drawable :as drawable]
            [com.badlogic.gdx.scenes.scene2d.utils.listener :as listener]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [gdl.math.vector2 :as v]
            [gdl.tx-handler :as tx-handler]
            [gdl.utils :as utils]
            [qrecord.core :as q]))

(def starting-world-fn "world_fns/vampire.edn")

(def ^:private sound-names (->> "sounds.edn" io/resource slurp edn/read-string))
(def ^:private path-format "sounds/%s.wav")

(def ^:private graphics-config
  {:tile-size 48
   :ui-viewport {:width 1440
                 :height 900}
   :world-viewport {:width 1440
                    :height 900}
   :texture-folder {:folder "resources/"
                    :extensions #{"png" "bmp"}}
   :default-font {:path "exocet/films.EXL_____.ttf"
                  :params {:size 16
                           :quality-scaling 2
                           :enable-markup? true
                           :use-integer-positions? false
                           ; :texture-filter/linear because scaling to world-units
                           :min-filter :linear
                           :mag-filter :linear}}
   :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
   :cursors {:path-format "cursors/%s.png"
             :data {:cursors/bag                   ["bag001"       [0   0]]
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
                    :cursors/walking               ["walking"      [16 16]]}}})

(def ^:private schema
  (m/schema
   [:map {:closed true}
    [:ctx/audio :some]
    [:ctx/db :some]
    [:ctx/graphics :some]
    [:ctx/input :some]
    [:ctx/stage :some]
    [:ctx/vis-ui :some]
    [:ctx/world :some]]))

(def zoom-speed 0.025)

(def pausing? true)

(def state->pause-game? {:stunned false
                         :player-moving false
                         :player-item-on-cursor true
                         :player-idle true
                         :player-dead true
                         :active-skill false})

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(def ^:private render-layers
  '[{:entity/mouseover?     cdq.entity.mouseover/draw
     :stunned               cdq.entity.state.stunned/draw
     :player-item-on-cursor cdq.entity.state.player-item-on-cursor/draw}
    {:entity/clickable      cdq.entity.clickable/draw
     :entity/animation      cdq.entity.animation/draw
     :entity/image          cdq.entity.image/draw
     :entity/line-render    cdq.entity.line/draw}
    {:npc-sleeping          cdq.entity.state.npc-sleeping/draw
     :entity/temp-modifier  cdq.entity.temp-modifier/draw
     :entity/string-effect  cdq.entity.string-effect/draw}
    {:entity/stats        cdq.entity.stats/draw
     :active-skill          cdq.entity.state.active-skill/draw}])

(alter-var-root #'render-layers
                (fn [k->fns]
                  (map (fn [k->fn]
                         (update-vals k->fn
                                      (fn [sym]
                                        (let [avar (requiring-resolve sym)]
                                          (assert avar sym)
                                          avar))))
                       k->fns)))

(def destroy-components
  {:entity/destroy-audiovisual
   {:destroy! (fn [audiovisuals-id eid _ctx]
                [[:tx/audiovisual
                  (:body/position (:entity/body @eid))
                  audiovisuals-id]])}})

(def ^:private k->tick-fn
  (update-vals
   '{:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick
     :entity/animation                       cdq.entity.animation/tick
     :entity/delete-after-duration           cdq.entity.delete-after-duration/tick
     :entity/movement                        cdq.entity.movement/tick
     :entity/projectile-collision            cdq.entity.projectile-collision/tick
     :entity/skills                          cdq.entity.skills/tick
     :active-skill                           cdq.entity.state.active-skill/tick
     :npc-idle                               cdq.entity.state.npc-idle/tick
     :npc-moving                             cdq.entity.state.npc-moving/tick
     :npc-sleeping                           cdq.entity.state.npc-sleeping/tick
     :stunned                                cdq.entity.state.stunned/tick
     :entity/string-effect                   cdq.entity.string-effect/tick
     :entity/temp-modifier                   cdq.entity.temp-modifier/tick}
   (fn [sym]
     (let [avar (requiring-resolve sym)]
       (assert avar sym)
       avar))))

(def ^:dbg-flag show-body-bounds? false)

(def ^:private txs-fn-map
  '{
    :tx/assoc (fn [_ctx eid k value]
                (swap! eid assoc k value)
                nil)
    :tx/assoc-in (fn [_ctx eid ks value]
                   (swap! eid assoc-in ks value)
                   nil)
    :tx/dissoc (fn [_ctx eid k]
                 (swap! eid dissoc k)
                 nil)
    :tx/mark-destroyed (fn [_ctx eid]
                         (swap! eid assoc :entity/destroyed? true)
                         nil)
    :tx/mod-add cdq.tx.mod-add/do!
    :tx/mod-remove cdq.tx.mod-remove/do!
    :tx/pay-mana-cost cdq.tx.pay-mana-cost/do!
    :tx/set-cooldown cdq.tx.set-cooldown/do!
    :tx/add-text-effect cdq.tx.add-text-effect/do!
    :tx/add-skill cdq.tx.add-skill/do!
    :tx/set-item cdq.tx.set-item/do!
    :tx/remove-item cdq.tx.remove-item/do!
    :tx/pickup-item cdq.tx.pickup-item/do!
    :tx/event cdq.tx.event/do!
    :tx/state-exit cdq.tx.state-exit/do!
    :tx/state-enter cdq.tx.state-enter/do!
    :tx/effect cdq.tx.effect/do!
    :tx/audiovisual cdq.tx.audiovisual/do!
    :tx/spawn-alert cdq.tx.spawn-alert/do!
    :tx/spawn-line cdq.tx.spawn-line/do!
    :tx/move-entity cdq.tx.move-entity/do!
    :tx/spawn-projectile cdq.tx.spawn-projectile/do!
    :tx/spawn-effect cdq.tx.spawn-effect/do!
    :tx/spawn-item     cdq.tx.spawn-item/do!
    :tx/spawn-creature cdq.tx.spawn-creature/do!
    :tx/spawn-entity   cdq.tx.spawn-entity/do!

    :tx/sound (fn [{:keys [ctx/audio]} sound-name]
                (audio/play-sound! audio sound-name)
                nil)
    :tx/print-stacktrace (let [print-level 3
                               print-depth 24]
                           (fn [_ctx throwable]
                             (binding [*print-level* print-level]
                               (pretty-repl/pretty-pst throwable print-depth))
                             nil))
    :tx/show-error-window        cdq.tx.stage/show-error-window!
    :tx/toggle-inventory-visible cdq.tx.stage/toggle-inventory-visible!
    :tx/show-message             cdq.tx.stage/show-message!
    :tx/show-modal               cdq.tx.stage/show-modal!
    }
  )

(alter-var-root #'txs-fn-map update-vals
                (fn [form]
                  (if (symbol? form)
                    (let [avar (requiring-resolve form)]
                      (assert avar form)
                      avar)
                    (eval form))))


(require 'cdq.tx.stage)

(def ^:private reaction-txs-fn-map
  {

   :tx/set-item (fn [ctx eid cell item]
                  (when (:entity/player? @eid)
                    (cdq.tx.stage/player-set-item! ctx cell item)
                    nil))

   :tx/remove-item (fn [ctx eid cell]
                     (when (:entity/player? @eid)
                       (cdq.tx.stage/player-remove-item! ctx cell)
                       nil))

   :tx/add-skill (fn [ctx eid skill]
                   (when (:entity/player? @eid)
                     (cdq.tx.stage/player-add-skill! ctx skill)
                     nil))
   }
  )

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs!
   ctx
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:world/player-components world)]
                                          {:position (mapv (partial + 0.5) (:world/start-position world))
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

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
                                                          (ctx/reset-game-state! ctx world-fn)))})}
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
                                    (ctx/handle-txs! ctx txs)))))
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
                            (render-hpmana-bar x y-hp   hpcontent-file   (modifiers/get-hitpoints stats) "HP")
                            (render-hpmana-bar x y-mana manacontent-file (modifiers/get-mana      stats) "MP"))))]
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

(defn- reset-stage-actors!
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

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(graphics/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(defn- reset-world-state
  [{:keys [ctx/db
           ctx/graphics]
    :as ctx}
   world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (update ctx :ctx/world world/reset-state world-fn-result)))

(q/defrecord Context []
  ctx/Validation
  (validate [ctx]
    (m/validate-humanize schema ctx)
    ctx)

  ctx/TransactionHandler
  (handle-txs! [ctx transactions]
    (let [handled-txs (tx-handler/actions!
                       txs-fn-map
                       ctx  ; here pass only world ....
                       transactions)]
      (tx-handler/actions!
       reaction-txs-fn-map
       ctx
       handled-txs
       :strict? false)))

  com.badlogic.gdx.scenes.scene2d.ctx/Graphics
  (draw! [{:keys [ctx/graphics]} draws]
    (graphics/handle-draws! graphics draws))

  ctx/ResetGameState
  (reset-game-state! [{:keys [ctx/world]
                       :as ctx}
                      world-fn]
    (disposable/dispose! world)
    (-> ctx
        reset-stage-actors!
        (reset-world-state world-fn)
        spawn-player!
        spawn-enemies!)))

(defn- create-record [ctx]
  (merge (map->Context {})
         ctx))

(def ^:private schema-fn-map
  '{
    :s/animation {cdq.db.schema/malli-form   cdq.db.schema.animation/malli-form
                  cdq.db.schema/create-value cdq.db.schema.animation/create-value
                  cdq.db.schema/create       cdq.db.schema.animation/create
                  cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/boolean {cdq.db.schema/malli-form   cdq.db.schema.boolean/malli-form
                cdq.db.schema/create-value cdq.db.schema.boolean/create-value
                cdq.db.schema/create       cdq.db.schema.boolean/create
                cdq.db.schema/value        cdq.db.schema.boolean/value}

    :s/enum {cdq.db.schema/malli-form   cdq.db.schema.enum/malli-form
             cdq.db.schema/create-value cdq.db.schema.enum/create-value
             cdq.db.schema/create       cdq.db.schema.enum/create
             cdq.db.schema/value        cdq.db.schema.enum/value}

    :s/image {cdq.db.schema/malli-form   cdq.db.schema.image/malli-form
              cdq.db.schema/create-value cdq.db.schema.image/create-value
              cdq.db.schema/create       cdq.db.schema.image/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/map {cdq.db.schema/malli-form   cdq.db.schema.map/malli-form
            cdq.db.schema/create-value cdq.db.schema.map/create-value
            cdq.db.schema/create       cdq.db.schema.map/create
            cdq.db.schema/value        cdq.db.schema.map/value}

    :s/number {cdq.db.schema/malli-form   cdq.db.schema.number/malli-form
               cdq.db.schema/create-value cdq.db.schema.number/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.edn/create
               cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/one-to-many {cdq.db.schema/malli-form   cdq.db.schema.one-to-many/malli-form
                    cdq.db.schema/create-value cdq.db.schema.one-to-many/create-value
                    cdq.db.schema/create       cdq.db.schema.one-to-many/create
                    cdq.db.schema/value        cdq.db.schema.one-to-many/value}

    :s/one-to-one {cdq.db.schema/malli-form   cdq.db.schema.one-to-one/malli-form
                   cdq.db.schema/create-value cdq.db.schema.one-to-one/create-value
                   cdq.db.schema/create       cdq.db.schema.one-to-one/create
                   cdq.db.schema/value        cdq.db.schema.one-to-one/value}

    :s/qualified-keyword {cdq.db.schema/malli-form   cdq.db.schema.qualified-keyword/malli-form
                          cdq.db.schema/create-value cdq.db.schema.qualified-keyword/create-value
                          cdq.db.schema/create       cdq.ui.editor.widget.default/create
                          cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/some {cdq.db.schema/malli-form   cdq.db.schema.some/malli-form
             cdq.db.schema/create-value cdq.db.schema.some/create-value
             cdq.db.schema/create       cdq.ui.editor.widget.default/create
             cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/sound {cdq.db.schema/malli-form   cdq.db.schema.sound/malli-form
              cdq.db.schema/create-value cdq.db.schema.sound/create-value
              cdq.db.schema/create       cdq.db.schema.sound/create
              cdq.db.schema/value        cdq.ui.editor.widget.default/value}

    :s/string {cdq.db.schema/malli-form   cdq.db.schema.string/malli-form
               cdq.db.schema/create-value cdq.db.schema.string/create-value
               cdq.db.schema/create       cdq.db.schema.string/create
               cdq.db.schema/value        cdq.db.schema.string/value}

    :s/val-max {cdq.db.schema/malli-form   cdq.db.schema.val-max/malli-form
                cdq.db.schema/create-value cdq.db.schema.val-max/create-value
                cdq.db.schema/create       cdq.ui.editor.widget.edn/create
                cdq.db.schema/value        cdq.ui.editor.widget.edn/value}

    :s/vector {cdq.db.schema/malli-form   cdq.db.schema.vector/malli-form
               cdq.db.schema/create-value cdq.db.schema.vector/create-value
               cdq.db.schema/create       cdq.ui.editor.widget.default/create
               cdq.db.schema/value        cdq.ui.editor.widget.default/value}
    }
  )

(alter-var-root #'schema-fn-map update-vals (fn [method-map]
                                              (update-vals method-map
                                                           (fn [sym]
                                                             (let [avar (requiring-resolve sym)]
                                                               (assert avar sym)
                                                               avar)))))

(defn create-db [ctx]
  (assoc ctx :ctx/db (cdq.application.create.db/create {:schemas "schema.edn"
                                                        :properties "properties.edn"
                                                        :schema-fn-map schema-fn-map})))

(defn- create-graphics!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}]
  (assoc ctx :ctx/graphics (graphics/create! files
                                             graphics
                                             graphics-config)))
(defn create-vis-ui! [ctx]
  (assoc ctx :ctx/vis-ui (vis-ui/load! {:skin-scale :x1})))

(defn- create-stage
  [{:keys [ctx/graphics]
    :as ctx}]
  (assoc ctx :ctx/stage (stage/create (:graphics/ui-viewport graphics)
                                      (:graphics/batch       graphics))))

(defn- create-input! [{:keys [ctx/input
                              ctx/stage]
                       :as ctx}]
  (assoc ctx :ctx/input (cdq.application.create.input/create! input stage)))

(defn- create-audio [{:keys [ctx/audio
                             ctx/files]
                      :as ctx}]
  (assoc ctx :ctx/audio (audio/create audio
                                      files
                                      sound-names
                                      path-format)))

(defn- dissoc-files [ctx]
  (dissoc ctx :ctx/files))

(defn- create-world [ctx]
  (assoc ctx :ctx/world (cdq.application.create.world/create)))

(defn- try-fetch-state-ctx
  [{:keys [ctx/stage]
    :as ctx}]
  (if-let [new-ctx (stage/get-ctx stage)]
    new-ctx
    ctx)) ; first render stage doesnt have context

(defn- update-mouse
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)]
    (update ctx :ctx/graphics #(-> %
                                   (graphics/unproject-ui    mouse-position)
                                   (graphics/unproject-world mouse-position)))))

(defn- update-mouseover-eid!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (let [mouseover-actor (cdq.stage/mouseover-actor stage (input/mouse-position input))
        {:keys [world/grid
                world/mouseover-eid
                world/player-eid]} world
        new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities grid (:graphics/world-mouse-position graphics)))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc-in ctx [:ctx/world :world/mouseover-eid] new-eid)))

; TODO also items/skills/mouseover-actors
; -> can separate function get-mouseover-item-for-debug (@ ctx)
(defn- check-open-debug!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage
           ctx/world]
    :as ctx}]
  (when (input/open-debug-button-pressed? input)
    (let [data (or (and (:world/mouseover-eid world) @(:world/mouseover-eid world))
                   @((:world/grid world) (mapv int (:graphics/world-mouse-position graphics))))]
      (stage/add! stage (scene2d/build
                         {:actor/type :actor.type/data-viewer
                          :title "Data View"
                          :data data
                          :width 500
                          :height 500}))))
  ctx)

(defn- assoc-active-entities
  [{:keys [ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/active-entities]
            (content-grid/active-entities (:world/content-grid world)
                                          @(:world/player-eid world))))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/set-camera-position! graphics
                                 (:body/position
                                  (:entity/body
                                   @(:world/player-eid world))))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear! graphics color/black)
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
                             {:ray-blocked? (partial world/ray-blocked? world)
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (graphics/camera-position graphics)
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)


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
          (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity)
                                                           (if (:body/collides? (:entity/body entity))
                                                             color/white
                                                             color/gray))))
        (doseq [[k v] entity
                :let [draw-fn (get render-layer k)]
                :when draw-fn]
          (graphics/handle-draws! graphics (draw-fn v entity ctx))))
       (catch Throwable t
         (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) color/red))
         (ctx/handle-txs! ctx
                          [[:tx/print-stacktrace t]]))))

(defn- draw-entities
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [entities (map deref (world/active-eids world))
        player @(:world/player-eid world)
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(defn- draw-tile-grid
  [{:keys [ctx/graphics]}]
  (when show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (graphics/camera-frustum graphics)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (graphics/world-viewport-width  graphics)))
        (+ 2 (int (graphics/world-viewport-height graphics)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-cell-debug
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

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world]}]
  (let [[x y] (mapv int (:graphics/world-mouse-position graphics))
        cell ((:world/grid world) [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(comment
 (require '[gdl.math.geom :as geom]
          '[cdq.world.grid :as grid])

 (defn- geom-test
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
            [:draw/rectangle x y width height [0 0 1 1]])))))

(defn- draw-on-world-viewport!* [{:keys [ctx/graphics]
              :as ctx}]
  (doseq [f [
             draw-tile-grid
             draw-cell-debug
             draw-entities
             #_geom-test
             highlight-mouseover-tile
             ]]
    (graphics/handle-draws! graphics (f ctx))))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics]
    :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (draw-on-world-viewport!* ctx)))
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
   [:interaction-state/mouseover-actor (cdq.stage/actor-information stage mouseover-actor)]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (< (body/distance (:entity/body @player-eid)
                                        (:entity/body @mouseover-eid))
                         (:entity/click-distance-tiles @player-eid))}]

   :else
   (if-let [skill-id (cdq.stage/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (creature/skill-usable-state entity skill effect-ctx)]
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
                                                       (cdq.stage/mouseover-actor stage (input/mouse-position input)))))

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        cursor-key (state/cursor [state-k (state-k entity)] eid ctx)]
    (graphics/set-cursor! graphics cursor-key))
  ctx)

(defn- player-state-handle-input!
  [{:keys [ctx/world]
    :as ctx}]
  (let [eid (:world/player-eid world)
        entity @eid
        state-k (:state (:entity/fsm entity))
        txs (state/handle-input [state-k (state-k entity)] eid ctx)]
    (ctx/handle-txs! ctx txs))
  ctx)

(defn- dissoc-interaction-state [ctx]
  (dissoc ctx :ctx/interaction-state))

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/world]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (or #_error
                (and pausing?
                     (state->pause-game? (:state (:entity/fsm @(:world/player-eid world))))
                     (not (input/unpause? input))))))

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
     (cdq.application.render.update-potential-fields/do! world)
     ctx)))

(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (doseq [eid (:world/active-entities world)
                 [k v] @eid
                 :let [f (k->tick-fn k)]
                 :when f]
           (ctx/handle-txs! ctx (f v eid world)))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))
        ctx)))

(defn- remove-destroyed-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (let [{:keys [world/content-grid
                world/entity-ids
                world/grid]} world]
    (doseq [eid (filter (comp :entity/destroyed? deref)
                        (vals @entity-ids))]
      (let [id (:entity/id @eid)]
        (assert (contains? @entity-ids id))
        (swap! entity-ids dissoc id))
      (content-grid/remove-entity! content-grid eid)
      (grid/remove-from-touched-cells! grid eid)
      (when (:body/collides? (:entity/body @eid))
        (grid/remove-from-occupied-cells! grid eid))
      (ctx/handle-txs! ctx
                       (mapcat (fn [[k v]]
                                 (when-let [destroy! (:destroy! (k destroy-components))]
                                   (destroy! v eid ctx)))
                               @eid))))
  ctx)

(defn- window-and-camera-controls!
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/zoom-in?            input) (graphics/change-zoom! graphics zoom-speed))
  (when (input/zoom-out?           input) (graphics/change-zoom! graphics (- zoom-speed)))
  (when (input/close-windows?      input) (cdq.stage/close-all-windows!         stage))
  (when (input/toggle-inventory?   input) (cdq.stage/toggle-inventory-visible!  stage))
  (when (input/toggle-entity-info? input) (cdq.stage/toggle-entity-info-window! stage))
  ctx)

(defn- render-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (stage/set-ctx! stage ctx)
  (stage/act!     stage)
  (stage/draw!    stage)
  (stage/get-ctx  stage))

(defn create! [ctx]
  (-> ctx
      create-record
      create-db
      create-graphics!
      create-vis-ui!
      create-stage
      create-input!
      create-audio
      dissoc-files
      create-world
      (ctx/reset-game-state! starting-world-fn)))

(defn dispose! [{:keys [ctx/audio
                        ctx/graphics
                        ctx/vis-ui
                        ctx/world]}]
  (disposable/dispose! audio)
  (disposable/dispose! graphics)
  (disposable/dispose! vis-ui)
  (disposable/dispose! world))

(defn render! [ctx]
  (-> ctx
      try-fetch-state-ctx
      ctx/validate
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
      dissoc-interaction-state
      assoc-paused
      update-world-time
      update-potential-fields!
      tick-entities!
      remove-destroyed-entities!
      window-and-camera-controls!
      render-stage!
      ctx/validate))

(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/update-viewports! graphics width height))
