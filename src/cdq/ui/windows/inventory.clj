(ns cdq.ui.windows.inventory
  (:require [cdq.grid2d :as g2d]
            [cdq.inventory :as inventory]
            [cdq.image :as image]
            [cdq.ui.group :as group]
            [cdq.ui.image :as ui.image]
            [cdq.ui.tooltip :as tooltip]
            [cdq.ui :as ui]
            [cdq.ui.utils :as utils]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.image]))

(defn create
  [{:keys [ctx/ui-viewport
           ctx/textures]}
   {:keys [title
           id
           visible?
           clicked-cell-fn]}]
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
                                 (image/texture-region {:image/file "images/items.png"
                                                        :image/bounds bounds}
                                                       textures)))
        cell-size 48
        slot->drawable (fn [slot]
                         (utils/drawable (slot->texture-region slot)
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
                           (fn [actor {:keys [ctx/ui-mouse-position
                                              ctx/player-eid]}]
                             (draw-cell-rect @player-eid
                                             (actor/get-x actor)
                                             (actor/get-y actor)
                                             (actor/hit actor ui-mouse-position)
                                             (actor/user-object (actor/parent actor))))})
        ->cell (fn [slot & {:keys [position]}]
                 (let [cell [slot (or position [0 0])]
                       background-drawable (slot->drawable slot)]
                   {:actor {:actor/type :actor.type/stack
                            :name "inventory-cell"
                            :user-object cell
                            :click-listener (clicked-cell-fn cell)
                            :actors [(draw-rect-actor)
                                     (ui.image/create background-drawable
                                                      {:name "image-widget"
                                                       :user-object {:background-drawable background-drawable
                                                                     :cell-size cell-size}})]}}))]
    (ui/window {:title title
                :id id
                :visible? visible?
                :pack? true
                :position [(:viewport/width ui-viewport)
                           (:viewport/height ui-viewport)]
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
        drawable (utils/drawable texture-region :width cell-size :height cell-size)]
    (clojure.gdx.scenes.scene2d.ui.image/set-drawable! image-widget drawable)
    (tooltip/add! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (group/find-actor cell-widget "image-widget")]
    (clojure.gdx.scenes.scene2d.ui.image/set-drawable! image-widget (:background-drawable (actor/user-object image-widget)))
    (tooltip/remove! cell-widget)))

(defn cell-with-item?
  "If the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (actor/parent actor)
       (= "inventory-cell" (actor/get-name (actor/parent actor)))
       (actor/user-object (actor/parent actor))))
