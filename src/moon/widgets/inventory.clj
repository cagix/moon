(ns moon.widgets.inventory
  (:require [data.grid2d :as g2d]
            [gdl.graphics.color :as color]
            [gdl.ui :as ui]
            [gdl.ui.actor :as a]
            [moon.component :refer [defc] :as component]
            [moon.entity :as entity]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.image :as image]
            [moon.graphics.shape-drawer :as sd]
            [moon.item :refer [valid-slot? empty-inventory]]
            [moon.stage :as stage]
            [moon.world :as world]))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity x y mouseover? cell]
  (sd/rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (sd/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui/widget
   (fn [this]
     (draw-cell-rect @world/player
                     (a/x this)
                     (a/y this)
                     (a/mouseover? this (gui-view/mouse-position))
                     (a/id (a/parent this))))))

(defn- player-clicked-inventory [cell]
  (entity/clicked-inventory-cell (entity/state-obj @world/player) cell))

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
  (-> (image/sprite-sheet "images/items.png" 48 48)
      (image/sprite (slot->sprite-idx slot))))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     ui/texture-region-drawable)]
    (ui/set-min-size! drawable cell-size)
    (ui/tinted-drawable drawable (color/create 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (ui/image-widget (slot->background slot) {:id :image})
        stack (ui/stack [(draw-rect-actor)
                         image-widget])]
    (a/set-name! stack "inventory-cell")
    (a/set-id! stack cell)
    (a/add-listener! stack (proxy [com.badlogic.gdx.scenes.scene2d.utils.ClickListener] []
                             (clicked [event x y]
                               (component/->handle (player-clicked-inventory cell)))))
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
    (doseq [y (range (g2d/height (:inventory.slot/bag empty-inventory)))]
      (doseq [x (range (g2d/width (:inventory.slot/bag empty-inventory)))]
        (.add table (->cell :inventory.slot/bag :position [x y])))
      (.row table))
    table))

(defc :widgets/inventory
  (component/create [_]
    (ui/window {:title "Inventory"
                :id :inventory-window
                :visible? false
                :pack? true
                :position [(gui-view/width) (gui-view/height)]
                :rows [[{:actor (inventory-table)
                         :pad 4}]]})))

(defn- cell-widget [cell]
  (get (::table (world/get-window :inventory-window))
       cell))

(defc :tx/set-item-image-in-widget
  (component/handle [[_ cell item]]
    (let [cell-widget (cell-widget cell)
          image-widget (get cell-widget :image)
          drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
      (ui/set-min-size! drawable cell-size)
      (ui/set-drawable! image-widget drawable)
      (ui/add-tooltip! cell-widget #(component/->info item))
      nil)))

(defc :tx/remove-item-from-widget
  (component/handle [[_ cell]]
    (let [cell-widget (cell-widget cell)
          image-widget (get cell-widget :image)]
      (ui/set-drawable! image-widget (slot->background (cell 0)))
      (ui/remove-tooltip! cell-widget)
      nil)))
