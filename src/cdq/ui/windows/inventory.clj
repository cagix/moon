(ns cdq.ui.windows.inventory
  (:require [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.ctx :as ctx]
            [clojure.gdx.graphics.color :as color]
            [gdl.ui :as ui]
            [gdl.c :as c]
            [gdl.graphics :as g]
            [cdq.utils :as utils]))

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
    (fn [actor {:keys [ctx/graphics
                       ctx/player-eid] :as ctx}]
      (g/handle-draws! graphics
                       (draw-cell-rect @player-eid
                                       (ui/get-x actor)
                                       (ui/get-y actor)
                                       (ui/hit actor (c/ui-mouse-position ctx))
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

(defn- slot->sprite [graphics slot]
  (g/sprite-sheet->sprite graphics
                          (g/sprite-sheet graphics "images/items.png" 48 48)
                          [21 (+ (slot->y-sprite-idx slot) 2)]))

(defn- slot->background [graphics slot]
  (ui/create-drawable (:sprite/texture-region (slot->sprite graphics slot))
                      :width cell-size
                      :height cell-size
                      :tint-color (color/create [1 1 1 0.4])))

(defn- ->cell [graphics slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        background-drawable (slot->background graphics slot)]
    (doto (ui/stack [(draw-rect-actor)
                     (ui/image-widget background-drawable
                                      {:name "image-widget"
                                       :user-object background-drawable})])
      (.setName "inventory-cell")
      (.setUserObject cell)
      (.addListener (ui/click-listener
                     (fn [{:keys [ctx/player-eid] :as ctx}]
                       (cdq.ctx/handle-txs! ctx (-> @player-eid
                                                        entity/state-obj
                                                        (state/clicked-inventory-cell player-eid cell)))))))))

(defn- inventory-table [g]
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->cell g :inventory.slot/helm)
                             (->cell g :inventory.slot/necklace)]
                            [nil
                             (->cell g :inventory.slot/weapon)
                             (->cell g :inventory.slot/chest)
                             (->cell g :inventory.slot/cloak)
                             (->cell g :inventory.slot/shield)]
                            [nil nil
                             (->cell g :inventory.slot/leg)]
                            [nil
                             (->cell g :inventory.slot/glove)
                             (->cell g :inventory.slot/rings :position [0 0])
                             (->cell g :inventory.slot/rings :position [1 0])
                             (->cell g :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag inventory/empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag inventory/empty-inventory)))]
                               (->cell g :inventory.slot/bag :position [x y]))))}))

(defn create [{:keys [ctx/graphics]} {:keys [title
                                             id
                                             visible?]}]
  (ui/window {:title title
              :id id
              :visible? visible?
              :pack? true
              :position [(:width (:ui-viewport graphics))
                         (:height (:ui-viewport graphics))]
              :rows [[{:actor (inventory-table graphics)
                       :pad 4}]]}))

(defn- get-cell-widget [inventory-window cell]
  (get (::table inventory-window) cell))

(defn set-item! [inventory-window cell item]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (ui/find-actor cell-widget "image-widget")
        drawable (ui/create-drawable (:sprite/texture-region (:entity/image item))
                                     :width cell-size
                                     :height cell-size)]
    (ui/set-drawable! image-widget drawable)
    (ui/add-tooltip! cell-widget #(cdq.ctx/info-text % item))))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get-cell-widget inventory-window cell)
        image-widget (ui/find-actor cell-widget "image-widget")]
    (ui/set-drawable! image-widget (ui/user-object image-widget))
    (ui/remove-tooltip! cell-widget)))

(defn cell-with-item?
  "If the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (ui/parent actor)
       (= "inventory-cell" (ui/get-name (ui/parent actor)))
       (ui/user-object (ui/parent actor))))
