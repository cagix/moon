(ns cdq.ui.inventory
  (:require [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.graphics :as graphics]
            [cdq.grid2d :as g2d]
            [cdq.info :as info]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [gdl.graphics]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Image)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable
                                                  TextureRegionDrawable
                                                  Drawable)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color   [0   0.6 0 0.8])
(def ^:private not-allowed-color [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity x y mouseover? cell]
  [[:draw/rectangle x y cell-size cell-size :gray]
   (when (and mouseover?
              (= :player-item-on-cursor (entity/state-k player-entity)))
     (let [item (:entity/item-on-cursor player-entity)
           color (if (inventory/valid-slot? cell item)
                   droppable-color
                   not-allowed-color)]
       [:draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color]))])

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui/widget
   {:draw
    (fn [^Actor actor {:keys [ctx/player-eid] :as ctx}]
      (g/handle-draws! ctx
                       (draw-cell-rect @player-eid
                                       (.getX actor)
                                       (.getY actor)
                                       (ui/hit actor (g/ui-mouse-position ctx))
                                       (ui/user-object (ui/parent actor)))))}))

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

; TODO actually we can pass this whole map into inventory-window ...
(defn- slot->sprite [{:keys [ctx/assets
                             ctx/world-unit-scale]} slot]
  (graphics/from-sheet (graphics/sprite-sheet (assets "images/items.png")
                                              48
                                              48
                                              world-unit-scale)
                       [21 (+ (slot->y-sprite-idx slot) 2)]
                       world-unit-scale))

(defn- slot->background [ctx slot]
  (let [drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (slot->sprite ctx slot)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (TextureRegionDrawable/.tint drawable (gdl.graphics/color 1 1 1 0.4))))

(defn- ->cell [ctx slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        background-drawable (slot->background ctx slot)]
    (doto (ui/stack [(draw-rect-actor)
                     (ui/image-widget background-drawable
                                      {:name "image-widget"
                                       :user-object background-drawable})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (ui/click-listener
                      (fn [{:keys [ctx/player-eid] :as ctx}]
                        (g/handle-txs! ctx (-> @player-eid
                                               entity/state-obj
                                               (state/clicked-inventory-cell player-eid cell)))))))))

(defn- inventory-table [ctx]
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->cell ctx :inventory.slot/helm)
                             (->cell ctx :inventory.slot/necklace)]
                            [nil
                             (->cell ctx :inventory.slot/weapon)
                             (->cell ctx :inventory.slot/chest)
                             (->cell ctx :inventory.slot/cloak)
                             (->cell ctx :inventory.slot/shield)]
                            [nil nil
                             (->cell ctx :inventory.slot/leg)]
                            [nil
                             (->cell ctx :inventory.slot/glove)
                             (->cell ctx :inventory.slot/rings :position [0 0])
                             (->cell ctx :inventory.slot/rings :position [1 0])
                             (->cell ctx :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->cell ctx :inventory.slot/bag :position [x y]))))}))

(defn create [ctx & {:keys [id position]}]
  (ui/window {:title "Inventory"
              :id id
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table ctx)
                       :pad 4}]]}))

(defn- get-cell-widget [inventory-window cell]
  (get (::table inventory-window) cell))

(defn set-item! [inventory-window cell item]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (ui/find-actor cell-widget "image-widget")
        drawable (TextureRegionDrawable. ^TextureRegion (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable (float cell-size) (float cell-size))
    (Image/.setDrawable image-widget drawable)
    (ui/add-tooltip! cell-widget #(info/text item %))))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (ui/find-actor cell-widget "image-widget")]
    (Image/.setDrawable image-widget (ui/user-object image-widget))
    (ui/remove-tooltip! cell-widget)))

(defn cell-with-item?
  "Ff the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (ui/parent actor)
       (= "inventory-cell" (Actor/.getName (ui/parent actor)))
       (ui/user-object (ui/parent actor))))
