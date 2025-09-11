(ns cdq.ui.windows.inventory
  (:require [cdq.inventory :as inventory]
            [cdq.stage :as stage]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.gdx.scenes.scene2d.ui.image :as image]
            [clojure.gdx.scenes.scene2d.utils :as utils]
            [clojure.vis-ui.tooltip :as tooltip]
            [clojure.vis-ui.widget :as widget]))


; TODO express the whole thing through a reduce !!!
; can simplifY!
(defn create
  [{:keys [ctx/stage]}
   {:keys [title
           id
           visible?
           clicked-cell-fn
           slot->texture-region
           ]}]
  (let [cell-size 48
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
                           :draw (fn [actor {:keys [ctx/ui-mouse-position
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
                                     (widget/image background-drawable
                                                   {:name "image-widget"
                                                    :user-object {:background-drawable background-drawable
                                                                  :cell-size cell-size}})]}}))]
    (widget/window {:title title
                    :id id
                    :visible? visible?
                    :pack? true
                    :position [(stage/viewport-width  stage)
                               (stage/viewport-height stage)]
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
                                                   (for [y (range 4)]
                                                     (for [x (range 6)]
                                                       (->cell :inventory.slot/bag :position [x y]))))}
                             :pad 4}]]})))

; TODO express the whole thing through a reduce !!!
; can simplifY!
(defn set-item!
  [inventory-window
   cell
   {:keys [texture-region
           tooltip-text]}]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (group/find-actor cell-widget "image-widget")
        cell-size (:cell-size (actor/user-object image-widget))
        drawable (utils/drawable texture-region :width cell-size :height cell-size)]
    (image/set-drawable! image-widget drawable)
    (tooltip/add! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (get (::table inventory-window) cell)
        image-widget (group/find-actor cell-widget "image-widget")]
    (image/set-drawable! image-widget (:background-drawable (actor/user-object image-widget)))
    (tooltip/remove! cell-widget)))

(defn cell-with-item?
  "If the actor is an inventory-cell, returns the inventory slot."
  [actor]
  {:pre [actor]}
  (and (actor/parent actor)
       (= "inventory-cell" (actor/get-name (actor/parent actor)))
       (actor/user-object (actor/parent actor))))
