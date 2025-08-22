(ns cdq.ui.windows.inventory
  (:require [cdq.ctx :as ctx]
            [cdq.grid2d :as g2d]
            [cdq.inventory :as inventory]
            [cdq.utils :as utils]
            [gdl.c :as c]
            [gdl.graphics :as g]
            [gdx.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdx.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Image)))

(defn create
  [{:keys [ctx/graphics]}
   {:keys [title
           id
           visible?
           state->clicked-inventory-cell]}]
  (let [slot->y-sprite-idx #:inventory.slot {:weapon   0
                                             :shield   1
                                             :rings    2
                                             :necklace 3
                                             :helm     4
                                             :cloak    5
                                             :chest    6
                                             :leg      7
                                             :glove    8
                                             :boot     9
                                             :bag      10} ; transparent
        slot->texture-region (fn [slot]
                               (let [width  48
                                     height 48
                                     sprite-x 21
                                     sprite-y (+ (slot->y-sprite-idx slot) 2)
                                     bounds [(* sprite-x width)
                                             (* sprite-y height)
                                             width
                                             height]]
                                 (g/image->texture-region graphics
                                                          {:image/file "images/items.png"
                                                           :image/bounds bounds})))
        cell-size 48
        slot->drawable (fn [slot]
                         (gdx.ui/drawable (slot->texture-region slot)
                                          :width cell-size
                                          :height cell-size
                                          :tint-color [1 1 1 0.4]))
        droppable-color   [0   0.6 0 0.8 1]
        not-allowed-color [0.6 0   0 0.8 1]
        draw-cell-rect (fn [player-entity x y mouseover? cell]
                         [[:draw/rectangle x y cell-size cell-size :gray]
                          (when (and mouseover?
                                     (= :player-item-on-cursor (:state (:entity/fsm player-entity))))
                            (let [item (:entity/item-on-cursor player-entity)
                                  color (if (inventory/valid-slot? cell item)
                                          droppable-color
                                          not-allowed-color)]
                              [:draw/filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color]))])
        ; TODO why do I need to call getX ?
        ; is not layouted automatically to cell , use 0/0 ??
        ; maybe .setTransform stack true ? , but docs say it should work anyway
        draw-rect-actor (fn []
                          {:actor/type :actor.type/widget
                           :draw
                           (fn [actor {:keys [ctx/graphics
                                              ctx/world] :as ctx}]
                             (g/handle-draws! graphics
                                              (draw-cell-rect @(:world/player-eid world)
                                                              (actor/get-x actor)
                                                              (actor/get-y actor)
                                                              (actor/hit actor (c/ui-mouse-position ctx))
                                                              (actor/user-object (actor/parent actor)))))})
        cell-click-listener
        (fn [cell]
          (fn [{:keys [ctx/world] :as ctx}]
            (ctx/handle-txs!
             ctx
             (let [player-eid (:world/player-eid world)]
               (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                 (f player-eid cell))))))
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :name "inventory-cell"
                            :user-object cell
                            :click-listener (cell-click-listener cell)
                            :actors [(draw-rect-actor)
                                     (ui/image-widget background-drawable
                                                      {:name "image-widget"
                                                       :user-object {:background-drawable background-drawable
                                                                     :cell-size cell-size}})]}}))]
    (ui/window {:title title
                :id id
                :visible? visible?
                :pack? true
                :position [(:viewport/width (:ui-viewport graphics))
                           (:viewport/height (:ui-viewport graphics))]
                :rows [[{:actor {:id ::table
                                 :actor/type :actor.type/table
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
                                                   (->cell :inventory.slot/bag :position [x y]))))}
                         :pad 4}]]})))

(defn set-item!
  [inventory-window
   cell
   {:keys [texture-region
           tooltip-text]}]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (group/find-actor cell-widget "image-widget")
        cell-size (:cell-size (actor/user-object image-widget))
        drawable (gdx.ui/drawable texture-region :width cell-size :height cell-size)]
    (Image/.setDrawable image-widget drawable)
    (actor/add-tooltip! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (group/find-actor cell-widget "image-widget")]
    (Image/.setDrawable image-widget (:background-drawable (actor/user-object image-widget)))
    (actor/remove-tooltip! cell-widget)))

(defn cell-with-item?
  "If the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (actor/parent actor)
       (= "inventory-cell" (actor/get-name (actor/parent actor)))
       (actor/user-object (actor/parent actor))))
