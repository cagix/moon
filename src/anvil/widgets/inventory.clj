(ns anvil.widgets.inventory
  (:require [anvil.component :as component]
            [gdl.context :as ctx]
            [anvil.entity :as entity]
            [gdl.graphics :as g]
            [anvil.info :as info]
            [anvil.widgets :as widgets]
            [anvil.world :as world]
            [gdl.stage :as stage]
            [gdl.ui :refer [set-drawable!
                            ui-widget
                            texture-region-drawable
                            image-widget
                            ui-stack
                            add-tooltip!
                            remove-tooltip!]
             :as ui]
            [gdl.val-max :as val-max]
            [gdl.ui.actor :refer [user-object] :as actor]
            [gdl.ui.utils :as scene2d.utils]
            [data.grid2d :as g2d])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.utils ClickListener)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity x y mouseover? cell]
  (g/rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (entity/valid-slot? cell item)
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
  (-> (ctx/sprite-sheet "images/items.png" 48 48)
      (ctx/from-sprite-sheet (slot->sprite-idx slot))))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     texture-region-drawable)]
    (scene2d.utils/set-min-size! drawable cell-size)
    (scene2d.utils/tint drawable (g/->color 1 1 1 0.4))))

(defn- ->cell ^Actor [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (image-widget (slot->background slot) {:id :image})
        stack (ui-stack [(draw-rect-actor)
                         image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [ClickListener] []
                          (clicked [event x y]
                            (component/clicked-inventory-cell
                             (entity/state-obj @world/player-eid)
                             cell))))
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
    (doseq [y (range (g2d/height (:inventory.slot/bag entity/empty-inventory)))]
      (doseq [x (range (g2d/width (:inventory.slot/bag entity/empty-inventory)))]
        (.add table (->cell :inventory.slot/bag :position [x y])))
      (.row table))
    table))

(defn-impl widgets/inventory []
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [ctx/viewport-width
                         ctx/viewport-height]
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- cell-widget [cell]
  (get (::table (stage/get-inventory)) cell))

(defn-impl widgets/set-item-image-in-widget [cell item]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (scene2d.utils/set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text item))))

(defn-impl widgets/remove-item-from-widget [cell]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background (cell 0)))
    (remove-tooltip! cell-widget)))
