(ns anvil.lifecycle.create
  (:require [anvil.component :refer [clicked-inventory-cell draw-gui-view]]
            [anvil.controls :as controls]
            [anvil.entity.inventory :as inventory]
            [anvil.entity.fsm :as fsm]
            [anvil.entity.hp :as hp]
            [anvil.entity.mana :as mana]
            [anvil.entity.skills :as skills]
            [gdl.graphics :as g]
            [gdl.graphics.camera :as cam]
            [anvil.info :as info]
            [anvil.level :refer [generate-level]]
            [anvil.world :as world]
            [gdl.stage :as stage]
            [gdl.graphics.sprite :as sprite]
            [gdl.ui :refer [ui-actor
                              set-drawable!
                              ui-widget
                              texture-region-drawable
                              image-widget
                              ui-stack
                              add-tooltip!
                              remove-tooltip!]
             :as ui]
            [gdl.val-max :as val-max]
            [gdl.ui.actor :refer [user-object] :as actor]
            [gdl.ui.group :refer [add-actor! find-actor]]
            [gdl.ui.utils :as scene2d.utils]
            [gdl.utils :refer [dev-mode? tile->middle bind-root readable-number]]
            [anvil.ui.player-message :as player-message]
            [anvil.world.content-grid :as content-grid]
            [data.grid2d :as g2d]
            [gdl.assets :refer [play-sound]]
            [gdl.db :as db]
            [gdl.tiled :as tiled])
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
        x (/ g/viewport-width 2)
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-mana 80 ; action-bar-icon-size
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (g/draw-image rahmen [x y])
                            (g/draw-image (sprite/sub contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                          [x y])
                            (render-infostr-on-bar (str (readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (hp/->value   player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (mana/->value player-entity) "MP")))})))

(defn- menu-item [text on-clicked]
  (doto (ui/menu-item text)
    (.addListener (ui/change-listener on-clicked))))

(defn- add-upd-label
  ([table text-fn icon]
   (let [icon (ui/image->widget (sprite/create icon) {})
         label (ui/label "")
         sub-table (ui/table {:rows [[icon label]]})]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table sub-table)))))
  ([table text-fn]
   (let [label (ui/label "")]
     (add-actor! table (ui-actor {:act #(.setText label (str (text-fn)))}))
     (.expandX (.right (Table/.add table label))))))

(defn- add-update-labels [menu-bar update-labels]
  (let [table (ui/menu-bar->table menu-bar)]
    (doseq [{:keys [label update-fn icon]} update-labels]
      (let [update-fn #(str label ": " (update-fn))]
        (if icon
          (add-upd-label table update-fn icon)
          (add-upd-label table update-fn))))))

(defn- add-menu [menu-bar {:keys [label items]}]
  (let [app-menu (ui/menu label)]
    (doseq [{:keys [label on-click]} items]
      (.addItem app-menu (menu-item label (or on-click (fn [])))))
    (ui/add-menu menu-bar app-menu)))

(defn- create-menu-bar [menus]
  (let [menu-bar (ui/menu-bar)]
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
                           :position [g/viewport-width 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (add-actor! window (ui-actor {:act (fn update-label-text []
                                         ; items then have 2x pretty-name
                                         #_(.setText (.getTitleLabel window)
                                                     (if-let [entity (world/mouseover-entity)]
                                                       (info/text [:property/pretty-name (:property/pretty-name entity)])
                                                       "Entity Info"))
                                         (.setText label
                                                   (str (when-let [entity (world/mouseover-entity)]
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
   {:menus [{:label "World"
             :items (for [world (db/build-all :properties/worlds)]
                      {:label (str "Start " (:property/id world))
                       :on-click #(create-world world)})}
            {:label "Help"
             :items [{:label controls/help-text}]}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn #(when-let [entity (world/mouseover-entity)]
                                   (:entity/id entity))
                     :icon "images/mouseover.png"}
                    {:label "elapsed-time"
                     :update-fn #(str (readable-number world/elapsed-time) " seconds")
                     :icon "images/clock.png"}
                    {:label "paused?"
                     :update-fn (fn [] world/paused?)}
                    {:label "GUI"
                     :update-fn g/mouse-position}
                    {:label "World"
                     :update-fn #(mapv int (g/world-mouse-position))}
                    {:label "Zoom"
                     :update-fn #(cam/zoom g/camera)
                     :icon "images/zoom.png"}
                    {:label "FPS"
                     :update-fn g/frames-per-second
                     :icon "images/fps.png"}]}))

(defn- dev-menu []
  (ui/table {:rows [[{:actor (ui/menu-bar->table (dev-menu-bar))
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
     (draw-cell-rect @world/player-eid
                     (.getX this)
                     (.getY this)
                     (actor/hit this (g/mouse-position))
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
    (scene2d.utils/tint drawable (g/->color 1 1 1 0.4))))

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
                            (clicked-inventory-cell (fsm/state-obj @world/player-eid) cell))))
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
              :position [g/viewport-width
                         g/viewport-height]
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
   (ui-actor {:draw #(draw-gui-view (fsm/state-obj @world/player-eid))})
   (player-message/actor)])

(defn dispose-world []
  (when (bound? #'world/tiled-map)
    (tiled/dispose world/tiled-map)))

(def ^:private ^:dbg-flag spawn-enemies? true)

(defn- spawn-enemies [tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (world/creature (update props :position tile->middle))))

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

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- ->raycaster [grid position->blocked?]
  (let [width  (g2d/width  grid)
        height (g2d/height grid)
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells grid)]
      (set-arr arr @cell position->blocked?))
    [arr width height]))

(defrecord RCell [position
                  middle ; only used @ potential-field-follow-to-enemy -> can remove it.
                  adjacent-cells
                  movement
                  entities
                  occupied
                  good
                  evil]
  world/Cell
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

(defn- ->world-grid [tiled-map]
  (g2d/create-grid
   (tiled/tm-width tiled-map)
   (tiled/tm-height tiled-map)
   (fn [position]
     (atom (grid-cell position
                      (case (tiled/movement-property tiled-map position)
                        "none" :none
                        "air"  :air
                        "all"  :all))))))

(defn- ->explored-tile-corners [tiled-map]
  (atom (g2d/create-grid
         (tiled/tm-width  tiled-map)
         (tiled/tm-height tiled-map)
         (constantly false))))

(defn- ->content-grid [tiled-map]
  (content-grid/create {:cell-size 16  ; FIXME global config
                        :width  (tiled/tm-width  tiled-map)
                        :height (tiled/tm-height tiled-map)}))

(defn- world-init [{:keys [tiled-map start-position]}]
  (bind-root world/tiled-map tiled-map)
  (bind-root world/explored-tile-corners (->explored-tile-corners tiled-map))
  (bind-root world/grid                  (->world-grid            tiled-map))
  (bind-root world/content-grid          (->content-grid          tiled-map))
  (bind-root world/entity-ids {})
  (bind-root world/raycaster (->raycaster world/grid world/blocks-vision?))
  (bind-root world/elapsed-time 0)
  (bind-root world/delta-time nil)
  (bind-root world/player-eid (world/creature (player-entity-props start-position)))
  (when spawn-enemies?
    (spawn-enemies tiled-map))
  (bind-root world/mouseover-eid nil))

(defn create-world [world-props]
  ; TODO assert is :screens/world
  (stage/reset (widgets))
  (dispose-world)
  (bind-root world/error nil)
  ; generate level -> creates actually the tiled-map and
  ; start-position?
  ; other stuff just depend on it?!
  (world-init (generate-level world-props)))
