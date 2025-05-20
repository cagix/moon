(ns cdq.ui.inventory
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.grid2d :as g2d]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [gdl.graphics]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Image
                                               Widget)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  TextureRegionDrawable
                                                  ClickListener
                                                  Drawable)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color   [0   0.6 0 0.8])
(def ^:private not-allowed-color [0.6 0   0 0.8])

(defn- draw-cell-rect! [player-entity x y mouseover? cell]
  (draw/rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (proxy [Widget] []
    (draw [_batch _parent-alpha]
      (let [^Actor actor this]
        (draw-cell-rect! @ctx/player-eid
                         (.getX actor)
                         (.getY actor)
                         (ui/hit actor (viewport/mouse-position ctx/ui-viewport))
                         (ui/user-object (ui/parent actor)))))))

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
  (graphics/from-sheet (graphics/sprite-sheet (ctx/assets "images/items.png") 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [slot]
  (let [drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (slot->sprite slot)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (gdl.graphics/color 1 1 1 0.4))))

(defn- ->cell [slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]]
    (doto (ui/stack [(draw-rect-actor)
                     (ui/image-widget (slot->background slot) {:id :image})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (proxy [ClickListener] []
                      (clicked [_event _x _y]
                        (-> @ctx/player-eid
                            entity/state-obj
                            (state/clicked-inventory-cell ctx/player-eid cell)
                            ctx/handle-txs!)))))))

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

(defn create [& {:keys [id position]}]
  (ui/window {:title "Inventory"
              :id id
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table)
                       :pad 4}]]}))

(defn- get-cell-widget [inventory-window cell]
  (get (::table inventory-window) cell))

(defn set-item! [inventory-window cell item]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (get cell-widget :image)
        drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item))))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (get cell-widget :image)]
    (Image/.setDrawable image-widget (slot->background (cell 0)))
    (ui/remove-tooltip! cell-widget)))

(defn cell-with-item? [actor]
  {:pre [actor]}
  (and (ui/parent actor)
       (= "inventory-cell" (Actor/.getName (ui/parent actor)))
       (get-in (:entity/inventory @ctx/player-eid)
               (ui/user-object (ui/parent actor)))))
