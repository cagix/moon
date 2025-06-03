(ns clojure.ui.windows.inventory
  (:require [clojure.gdx :as gdx]
            [clojure.entity :as entity]
            [clojure.grid2d :as g2d]
            [clojure.inventory :as inventory]
            [clojure.state :as state]
            [clojure.ctx :as ctx]
            [clojure.ui :as ui]
            [clojure.utils :as utils]))

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
    (fn [actor {:keys [ctx/player-eid] :as ctx}]
      (ctx/handle-draws! ctx
                         (draw-cell-rect @player-eid
                                         (ui/get-x actor)
                                         (ui/get-y actor)
                                         (ui/hit actor (ctx/ui-mouse-position ctx))
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
(defn- slot->sprite [ctx slot]
  (ctx/sprite-sheet->sprite ctx
                            (ctx/sprite-sheet ctx "images/items.png" 48 48)
                            [21 (+ (slot->y-sprite-idx slot) 2)]))

(defn- slot->background [ctx slot]
  (ui/create-drawable (:sprite/texture-region (slot->sprite ctx slot))
                      :width cell-size
                      :height cell-size
                      :tint-color (gdx/->color [1 1 1 0.4])))

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
                       (clojure.ctx/handle-txs! ctx (-> @player-eid
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

(defn create [{:keys [ctx/ui-viewport]
               :as ctx}]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [(:width ui-viewport)
                         (:height ui-viewport)]
              :rows [[{:actor (inventory-table ctx)
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
    (ui/add-tooltip! cell-widget #(clojure.ctx/info-text % item))))

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
