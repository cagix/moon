(ns cdq.c
  (:require cdq.scene2d.build.editor-overview-window
            cdq.scene2d.build.editor-window
            [clj-commons.pretty.repl :as pretty-repl]
            [cdq.ctx :as ctx]
            [clojure.edn :as edn]
            [clojure.scene2d.vis-ui :as vis-ui]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cdq.audio :as audio]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.stats :as stats]
            [cdq.entity.state :as state]
            cdq.entity.state.player-item-on-cursor
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.stage]
            [cdq.ui.action-bar]
            [cdq.ui.inventory]
            [cdq.val-max :as val-max]
            [cdq.world :as world]
            [cdq.world-fns.creature-tiles]
            [cdq.world.content-grid :as content-grid]
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
            [gdl.tx-handler :as tx-handler]
            [gdl.utils :as utils]))

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

(defn- create-db [ctx]
  (assoc ctx :ctx/db (db/create)))

(defn- create-graphics!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}
   params]
  (assoc ctx :ctx/graphics (graphics/create! files graphics params)))

(defn- create-vis-ui! [ctx params]
  (assoc ctx :ctx/vis-ui (vis-ui/load! params)))

(defn- create-stage
  [{:keys [ctx/graphics]
    :as ctx}]
  (assoc ctx :ctx/stage (stage/create (:graphics/ui-viewport graphics)
                                      (:graphics/batch       graphics))))

(defn- create-input! [{:keys [ctx/input
                              ctx/stage]
                       :as ctx}]
  (assoc ctx :ctx/input (input/create! input stage)))

(defn- create-audio [{:keys [ctx/audio
                             ctx/files]
                      :as ctx}
                     sound-names path-format]
  (assoc ctx :ctx/audio (audio/create audio
                                      files
                                      sound-names
                                      path-format)))

(defn- dissoc-files [ctx]
  (dissoc ctx :ctx/files))

(defn- create-world [ctx params]
  (assoc ctx :ctx/world (world/create params)))

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

(defn create! [ctx]
  (extend-type (class ctx)
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
          spawn-enemies!)) )
  (-> ctx
      create-db
      (create-graphics! graphics-config)
      (create-vis-ui! {:skin-scale :x1})
      create-stage
      create-input!
      (create-audio sound-names path-format)
      dissoc-files
      (create-world world-params)
      (ctx/reset-game-state! "world_fns/vampire.edn")))
