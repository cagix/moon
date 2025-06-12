(ns cdq.ui.windows.inventory
  (:require [cdq.entity :as entity]
            [cdq.grid2d :as g2d]
            [cdq.inventory :as inventory]
            [cdq.state :as state]
            [cdq.utils :as utils]
            [cdq.world :as world]
            [gdl.c :as c]
            [gdl.graphics :as g]
            [gdl.ui :as ui]))

(defn create
  [{:keys [ctx/graphics]}
   {:keys [title
           id
           visible?]}]
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
                         (ui/create-drawable (slot->texture-region slot)
                                             :width cell-size
                                             :height cell-size
                                             :tint-color [1 1 1 0.4]))
        droppable-color   [0   0.6 0 0.8 1]
        not-allowed-color [0.6 0   0 0.8 1]
        draw-cell-rect (fn [player-entity x y mouseover? cell]
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
        ; maybe .setTransform stack true ? , but docs say it should work anyway
        draw-rect-actor (fn []
                          {:actor/type :actor.type/widget
                           :draw
                           (fn [actor {:keys [ctx/graphics
                                              ctx/player-eid] :as ctx}]
                             (g/handle-draws! graphics
                                              (draw-cell-rect @player-eid
                                                              (ui/get-x actor)
                                                              (ui/get-y actor)
                                                              (ui/hit actor (c/ui-mouse-position ctx))
                                                              (ui/user-object (ui/parent actor)))))})
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :name "inventory-cell"
                            :user-object cell
                            :click-listener (fn [{:keys [ctx/player-eid] :as ctx}]
                                              (world/handle-txs! ctx (-> @player-eid
                                                                         entity/state-obj
                                                                         (state/clicked-inventory-cell player-eid cell))))
                            :actors [(draw-rect-actor)
                                     (ui/image-widget background-drawable
                                                      {:name "image-widget"
                                                       :user-object {:background-drawable background-drawable
                                                                     :cell-size cell-size}})]}}))]
    (ui/window {:title title
                :id id
                :visible? visible?
                :pack? true
                :position [(:width (:ui-viewport graphics))
                           (:height (:ui-viewport graphics))]
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
        image-widget (ui/find-actor cell-widget "image-widget")
        cell-size (:cell-size (ui/user-object image-widget))
        drawable (ui/create-drawable texture-region
                                     :width cell-size
                                     :height cell-size)]
    (ui/set-drawable! image-widget drawable)
    (ui/add-tooltip! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (ui/find-actor cell-widget "image-widget")]
    (ui/set-drawable! image-widget (:background-drawable (ui/user-object image-widget)))
    (ui/remove-tooltip! cell-widget)))

(defn cell-with-item?
  "If the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (ui/parent actor)
       (= "inventory-cell" (ui/get-name (ui/parent actor)))
       (ui/user-object (ui/parent actor))))
