(ns cdq.ui.inventory-window
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.inventory :as inventory]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [clojure.data.grid2d :as g2d]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Image Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable ClickListener)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color   [0   0.6 0 0.8])
(def ^:private not-allowed-color [0.6 0   0 0.8])

(defn- draw-cell-rect! [g player-entity x y mouseover? cell]
  (graphics/draw-rectangle g x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (graphics/draw-filled-rectangle g (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [g ctx/graphics]
        (draw-cell-rect! g
                         @ctx/player-eid
                         (actor/x this)
                         (actor/y this)
                         (actor/hit this (graphics/mouse-position g))
                         (actor/user-object (actor/parent this)))))))

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
  (graphics/from-sheet ctx/graphics
                       (graphics/sprite-sheet ctx/graphics (ctx/assets "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (-> (slot->sprite slot)
                     :texture-region
                     ui/texture-region-drawable)]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (color/create 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/ui-stack [(draw-rect-actor)
                        (ui/image-widget (slot->background slot) {:id :image})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (proxy [ClickListener] []
                      (clicked [_event _x _y]
                        (state/clicked-inventory-cell (entity/state-obj @ctx/player-eid) cell)))))))

(defn- inventory-table []
  (ui/table {:id ::table
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
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->cell :inventory.slot/bag :position [x y]))))}))

(defn create [position]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- get-cell-widget [cell]
  (get (::table (-> ctx/stage :windows :inventory-window)) cell))

(defn set-item-image! [cell item]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item))))

(defn remove-item-image! [cell]
  (let [cell-widget (get-cell-widget cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (ui/remove-tooltip! cell-widget)))
