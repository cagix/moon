(ns forge.world.create
  (:require [anvil.audio :refer [play-sound]]
            [anvil.content-grid :as content-grid]
            [anvil.controls :as controls]
            [anvil.db :as db]
            [anvil.entity :as entity :refer [player-eid mouseover-entity]]
            [anvil.error :as error]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g]
            [anvil.graphics.camera :as cam]
            [anvil.graphics.color :refer [->color]]
            [anvil.grid :as grid]
            [anvil.hitpoints :as hp]
            [anvil.info :as info]
            [anvil.inventory :as inventory]
            [anvil.item-on-cursor :refer [world-item?]]
            [anvil.level :as level :refer [generate-level]]
            [anvil.mana :as mana]
            [anvil.raycaster :as raycaster]
            [anvil.screen :as screen]
            [anvil.skills :as skills]
            [anvil.stage :as stage]
            [anvil.sprite :as sprite]
            [anvil.time :as time]
            [anvil.ui :refer [ui-actor
                              set-drawable!
                              ui-widget
                              texture-region-drawable
                              image-widget
                              ui-stack
                              add-tooltip!
                              remove-tooltip!]
             :as ui]
            [anvil.val-max :as val-max]
            [anvil.world :as world]
            [anvil.utils :refer [dispose defsystem]]
            [clojure.gdx :as gdx]
            [anvil.ui.actor :refer [user-object] :as actor]
            [anvil.ui.group :refer [add-actor! find-actor]]
            [anvil.ui.utils :as scene2d.utils]
            [anvil.tiled :as tiled]
            [anvil.utils :refer [dev-mode? tile->middle bind-root readable-number]]
            [clojure.vis-ui :as vis]
            [data.grid2d :as g2d]
            [forge.ui.player-message :as player-message])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Touchable)
           (com.badlogic.gdx.scenes.scene2d.ui Table Button ButtonGroup)
           (com.badlogic.gdx.scenes.scene2d.utils ClickListener)))

(defn- clicked-cell [eid cell]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/set-item eid cell item-on-cursor)
      (fsm/event eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/stack-item eid cell item-on-cursor)
      (fsm/event eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play-sound "bfxr_itemput")
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (inventory/remove-item eid cell)
      (inventory/set-item eid cell item-on-cursor)
      (fsm/event eid :dropped-item)
      (fsm/event eid :pickup-item item-in-cell)))))

(defn- render-infostr-on-bar [infostr x y h]
  (g/draw-text {:text infostr
                :x (+ x 75)
                :y (+ y 2)
                :up? true}))

(defn- hp-mana-bar []
  (let [rahmen      (sprite/create "images/rahmen.png")
        hpcontent   (sprite/create "images/hp.png")
        manacontent (sprite/create "images/mana.png")
        x (/ ui/viewport-width 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (g/draw-image rahmen [x y])
                            (g/draw-image (sprite/sub contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @entity/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hp/->value   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (mana/->value player-entity) "MP")))})))

(defn- menu-item [text on-clicked]
  (doto (vis/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (sprite/create icon) {})
         label (vis/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (vis/label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (vis/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (vis/menu label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (vis/add-menu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (vis/menu-bar)]
    (run! #(add-menu menu-bar %) menus)
    menu-bar))

(defn- dev-menu* [{:keys [menus update-labels]}]
  (let [menu-bar (create-menu-bar menus)]
    (add-update-labels menu-bar update-labels)
    menu-bar))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- entity-info-window []
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [ui/viewport-width 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (ui-actor {:act (fn update-label-text []
                                         ; items then have 2x pretty-name
                                         #_(.setText (.getTitleLabel window)
                                                     (if-let [entity (mouseover-entity)]
                                                       (info/text [:property/pretty-name (:property/pretty-name entity)])
                                                       "Entity Info"))
                                         (.setText label
                                                   (str (when-let [entity (mouseover-entity)]
                                                          (info/text
                                                           ; don't use select-keys as it loses Entity record type
                                                           (apply dissoc entity disallowed-keys)))))
                                         (.pack window))}))
    window))


(declare create-world)

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor?)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-bar []
  (dev-menu*
   {:menus [{:label "Screens"
             :items [{:label "Map-editor"
                      :on-click (partial screen/change :screens/map-editor)}
                     {:label "Editor"
                      :on-click (partial screen/change :screens/editor)}
                     {:label "Main-Menu"
                      :on-click (partial screen/change :screens/main-menu)}]}
            {:label "World"
             :items (for [world (db/build-all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(create-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (mouseover-entity)] (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number time/elapsed) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] time/paused?)}
                    {:label "GUI"
                     :update-fn ui/mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (world/mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom (world/camera))
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn gdx/frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (vis/menu-bar->table (dev-menu-bar))
                      :expand-x? true
                      :fill-x? true
                      :colspan 1}]
                    [{:actor (doto (ui/label "")
                               (.setTouchable Touchable/disabled))
                      :expand? true
                      :fill-x? true
                      :fill-y? true}]]
             :fill-parent? true}))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity x y mouseover? cell]
  (g/rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (fsm/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (g/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui-widget
   (fn [^Actor this]
     (draw-cell-rect @player-eid
                     (.getX this)
                     (.getY this)
                     (actor/hit this (ui/mouse-position))
                     (user-object (.getParent this))))))

(def ^:private slot->y-sprite-idx
  #:inventory.slot {:weapon   0
                    :shield   1
                    :rings    2
                    :necklace 3
                    :helm     4
                    :cloak    5
                    :chest    6
                    :leg      7
                    :glove    8
                    :boot     9
                    :bag      10}) ; transparent

(defn- slot->sprite-idx [slot]
  [21 (+ (slot->y-sprite-idx slot) 2)])

(defn- slot->sprite [slot]
  (-> (sprite/sheet "images/items.png" 48 48)
      (sprite/from-sheet (slot->sprite-idx slot))))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     texture-region-drawable)]
    (scene2d.utils/set-min-size! drawable cell-size)
    (scene2d.utils/tint drawable (->color 1 1 1 0.4))))

(defsystem clicked-inventory-cell)
(defmethod clicked-inventory-cell :default [_ cell])

(defmethod clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid]}] cell]
  (clicked-cell eid cell))

(defmethod clicked-inventory-cell :player-idle [[_ {:keys [eid]}] cell]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (play-sound "bfxr_takeit")
    (fsm/event eid :pickup-item item)
    (inventory/remove-item eid cell)))

(defn- ->cell ^Actor [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (image-widget (slot->background slot) {:id :image})
        stack (ui-stack [(draw-rect-actor)
                         image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [ClickListener] []
                          (clicked [event x y]
                            (clicked-inventory-cell (fsm/state-obj @player-eid) cell))))
    stack))

(defn- inventory-table []
  (let [table (ui/table {:id ::table})]
    (.clear table) ; no need as we create new table ... TODO
    (doto table .add .add
      (.add (->cell :inventory.slot/helm))
      (.add (->cell :inventory.slot/necklace)) .row)
    (doto table .add
      (.add (->cell :inventory.slot/weapon))
      (.add (->cell :inventory.slot/chest))
      (.add (->cell :inventory.slot/cloak))
      (.add (->cell :inventory.slot/shield)) .row)
    (doto table .add .add
      (.add (->cell :inventory.slot/leg)) .row)
    (doto table .add
      (.add (->cell :inventory.slot/glove))
      (.add (->cell :inventory.slot/rings :position [0 0]))
      (.add (->cell :inventory.slot/rings :position [1 0]))
      (.add (->cell :inventory.slot/boot)) .row)
    (doseq [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
      (doseq [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
        (.add table (->cell :inventory.slot/bag :position [x y])))
      (.row table))
    table))

(defn- create-inventory []
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [ui/viewport-width
                         ui/viewport-height]
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- cell-widget [cell]
  (get (::table (stage/get-inventory)) cell))

(defn- set-item-image-in-widget [cell item]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (scene2d.utils/set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text item))))

(defn- remove-item-from-widget [cell]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background (cell 0)))
    (remove-tooltip! cell-widget)))

(bind-root inventory/player-set-item    set-item-image-in-widget)
(bind-root inventory/player-remove-item remove-item-from-widget)

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (ui/button-group {:max-check-count 1
                                                  :min-check-count 0}))
    actor))

(defn- action-bar []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn- action-bar-add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (stage/get-action-bar)
        button (ui/image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info/text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn- action-bar-remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (stage/get-action-bar)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(bind-root skills/player-add-skill    action-bar-add-skill)
(bind-root skills/player-remove-skill action-bar-remove-skill)

(defsystem draw-gui-view)
(defmethod draw-gui-view :default [_])

(defmethod draw-gui-view :player-item-on-cursor [[_ {:keys [eid]}]]
  (when (not (world-item?))
    (g/draw-centered (:entity/image (:entity/item-on-cursor @eid))
                     (ui/mouse-position))))

(defn- widgets []
  [(if dev-mode?
     (dev-menu)
     (ui-actor {}))
   (ui/table {:rows [[{:actor (action-bar)
                       :expand? true
                       :bottom? true}]]
              :id :action-bar-table
              :cell-defaults {:pad 2}
              :fill-parent? true})
   (hp-mana-bar)
   (ui/group {:id :windows
              :actors [(entity-info-window)
                       (create-inventory)]})
   (ui-actor {:draw #(draw-gui-view (fsm/state-obj @entity/player-eid))})
   (player-message/actor)])

(defn dispose-world []
  (when (bound? #'level/tiled-map)
    (dispose level/tiled-map)))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (entity/creature (update props :position tile->middle))))

; player-creature needs mana & inventory
; till then hardcode :creatures/vampire
(defn- player-entity-props [start-position]
  {:position (tile->middle start-position)
   :creature-id :creatures/vampire
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? true
                :entity/free-skill-points 3
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles 1.5}})

(defn- time-init []
  (bind-root time/elapsed 0)
  (bind-root time/delta nil))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- init-raycaster* [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    (bind-root raycaster/raycaster [arr width height])))

(defn init-raycaster [tiled-map]
  (init-raycaster* grid/grid grid/blocks-vision?))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  grid/Cell
  (cell-blocked? [_ z-order]
    (case movement
      :none true ; wall
      :air (case z-order ; water/doodads
             :z-order/flying false
             :z-order/ground true)
      :all false)) ; ground/floor

  (blocks-vision? [_]
    (= movement :none))

  (occupied-by-other? [_ eid]
    (some #(not= % eid) occupied)) ; contains? faster?

  (nearest-entity [this faction]
    (-> this faction :eid))

  (nearest-entity-distance [this faction]
    (-> this faction :distance)))

(defn- grid-cell [position movement]
  {:pre [(#{:none :air :all} movement)]}
  (map->RCell
   {:position position
    :middle (tile->middle position)
    :movement movement
    :entities #{}
    :occupied #{}}))

(defn- init-world-grid [tiled-map]
  (bind-root grid/grid (g2d/create-grid
                        (tiled/tm-width tiled-map)
                        (tiled/tm-height tiled-map)
                        (fn [position]
                          (atom (grid-cell position
                                           (case (tiled/movement-property tiled-map position)
                                             "none" :none
                                             "air"  :air
                                             "all"  :all)))))))

(defn- world-init [{:keys [tiled-map start-position]}]
  (bind-root level/tiled-map tiled-map)
  (bind-root level/explored-tile-corners (atom (g2d/create-grid
                                                (tiled/tm-width  tiled-map)
                                                (tiled/tm-height tiled-map)
                                                (constantly false))))
  (init-world-grid tiled-map)
  (bind-root entity/ids {})
  (bind-root entity/content-grid
             (content-grid/create {:cell-size 16  ; FIXME global config
                                   :width  (tiled/tm-width  tiled-map)
                                   :height (tiled/tm-height tiled-map)}))
  (init-raycaster tiled-map)
  (time-init)
  (bind-root entity/player-eid
             (entity/creature
              (player-entity-props start-position)))
  (when spawn-enemies?
    (spawn-enemies tiled-map)))

(defn create-world [world-props]
  ; TODO assert is :screens/world
  (stage/reset (widgets))
  (dispose-world)
  (bind-root error/throwable nil)
  ; generate level -> creates actually the tiled-map and
  ; start-position?
  ; other stuff just depend on it?!
  (world-init (generate-level world-props)))
