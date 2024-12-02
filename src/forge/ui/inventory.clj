(ns forge.ui.inventory
  (:require [data.grid2d :as g2d]
            [forge.core :refer :all]
            [forge.entity.components :as entity]
            [forge.entity.state :as state]
            [forge.world :refer [player-eid]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity x y mouseover? cell]
  (draw-rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (draw-filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui-widget
   (fn [this]
     (draw-cell-rect @player-eid
                     (.getX this)
                     (.getY this)
                     (actor-hit this (gui-mouse-position))
                     (.getUserObject (.getParent this))))))

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
  (-> (sprite-sheet "images/items.png" 48 48)
      (->sprite (slot->sprite-idx slot))))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     texture-region-drawable)]
    (set-min-size! drawable cell-size)
    (tinted-drawable drawable (gdx-color 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (image-widget (slot->background slot) {:id :image})
        stack (ui-stack [(draw-rect-actor)
                         image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [com.badlogic.gdx.scenes.scene2d.utils.ClickListener] []
                             (clicked [event x y]
                               (state/clicked-inventory-cell (entity/state-obj @player-eid) cell))))
    stack))

(def empty-inventory
  (->> #:inventory.slot{:bag      [6 4]
                        :weapon   [1 1]
                        :shield   [1 1]
                        :helm     [1 1]
                        :chest    [1 1]
                        :leg      [1 1]
                        :glove    [1 1]
                        :boot     [1 1]
                        :cloak    [1 1]
                        :necklace [1 1]
                        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot (grid2d width height (constantly nil))]))
       (into {})))

(defn- inventory-table []
  (let [table (ui-table {:id ::table})]
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

(defn create []
  (ui-window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [gui-viewport-width gui-viewport-height]
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn window []
  (get (:windows (screen-stage)) :inventory-window))

(defn- cell-widget [cell]
  (get (::table (window)) cell))

(defn set-item-image-in-widget [cell item]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info-text item))))

(defn remove-item-from-widget [cell]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background (cell 0)))
    (remove-tooltip! cell-widget)))
