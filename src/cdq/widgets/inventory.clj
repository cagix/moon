(ns cdq.widgets.inventory
  (:require [cdq.entity :as entity]
            [cdq.entity.fsm :as fsm]
            gdl.graphics
            [cdq.inventory :refer [empty-inventory] :as inventory]
            [cdq.info :as info]
            [gdl.graphics.shape-drawer :as sd]
            gdl.graphics.sprite
            [gdl.gdx.scenes.scene2d.actor :refer [user-object] :as actor]
            [gdl.data.grid2d :as g2d]
            [cdq.ui :refer [ui-widget
                            texture-region-drawable
                            image-widget
                            ui-stack
                            set-drawable!
                            add-tooltip!
                            remove-tooltip!]
             :as ui]
            [gdl.audio.sound :as sound]
            [gdl.gdx.scenes.scene2d.ui.utils :as scene2d.utils])
  (:import (com.badlogic.gdx.graphics Color)))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [sd player-entity x y mouseover? cell]
  (sd/rectangle sd x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (sd/filled-rectangle sd (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui-widget
   (fn [this {:keys [cdq.context/player-eid
                     gdl.graphics/shape-drawer
                     gdl.graphics/ui-viewport]}]
     (draw-cell-rect shape-drawer
                     @player-eid
                     (actor/x this)
                     (actor/y this)
                     (actor/hit this (gdl.graphics/mouse-position ui-viewport))
                     (user-object (actor/parent this))))))

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

(defn- slot->sprite [c slot]
  (gdl.graphics.sprite/from-sheet (gdl.graphics.sprite/sheet c
                                                                     "images/items.png"
                                                                     48
                                                                     48)
                                      (slot->sprite-idx slot)
                                      c))

(defn- slot->background [c slot]
  (let [drawable (-> (slot->sprite c slot)
                     :texture-region
                     texture-region-drawable)]
    (scene2d.utils/set-min-size! drawable cell-size)
    (scene2d.utils/tint drawable (Color. (float 1) (float 1) (float 1) (float 0.4)))))

(defmulti clicked-inventory-cell (fn [[k] cell c]
                                   k))
(defmethod clicked-inventory-cell :default [_ cell c])

(defn- ->cell [c slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (image-widget (slot->background c slot)
                                   {:id :image})
        stack (ui-stack [(draw-rect-actor)
                         image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (ui/click-listener
                         (fn [_click-context]
                           (let [{:keys [cdq.context/player-eid] :as context} (ui/application-state stack)]
                             (clicked-inventory-cell (entity/state-obj @player-eid)
                                                     cell
                                                     context)))))
    stack))

(defn- inventory-table [c]
  (ui/table {:id ::table
             :rows (concat [[nil nil
                             (->cell c :inventory.slot/helm)
                             (->cell c :inventory.slot/necklace)]
                            [nil
                             (->cell c :inventory.slot/weapon)
                             (->cell c :inventory.slot/chest)
                             (->cell c :inventory.slot/cloak)
                             (->cell c :inventory.slot/shield)]
                            [nil nil
                             (->cell c :inventory.slot/leg)]
                            [nil
                             (->cell c :inventory.slot/glove)
                             (->cell c :inventory.slot/rings :position [0 0])
                             (->cell c :inventory.slot/rings :position [1 0])
                             (->cell c :inventory.slot/boot)]]
                           (for [y (range (g2d/height (:inventory.slot/bag empty-inventory)))]
                             (for [x (range (g2d/width (:inventory.slot/bag empty-inventory)))]
                               (->cell c :inventory.slot/bag :position [x y]))))}))

(defn create [{:keys [gdl.graphics/ui-viewport] :as c}]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [(:width  ui-viewport)
                         (:height ui-viewport)]
              :rows [[{:actor (inventory-table c)
                       :pad 4}]]}))

(defn- inventory-cell-widget [c cell]
  (get (::table (get (:windows (:cdq.context/stage c)) :inventory-window)) cell))

(defn- set-item-image-in-widget [c cell item]
  (let [cell-widget (inventory-cell-widget c cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (scene2d.utils/set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text % item))))

(defn- remove-item-from-widget [c cell]
  (let [cell-widget (inventory-cell-widget c cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background c (cell 0)))
    (remove-tooltip! cell-widget)))

(defn- set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (set-item-image-in-widget c cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn- remove-item [c eid cell]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (when (:entity/player? entity)
      (remove-item-from-widget c cell))
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! eid cell)
       (set-item! eid cell (update item :count dec)))
      (remove-item! eid cell))))

; TODO no items which stack are available
(defn- stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item c eid cell)
            (set-item c eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [c eid item]
  (let [[cell cell-item] (entity/can-pickup-item? @eid item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item c eid cell item)
      (set-item c eid cell item))))

(defn- clicked-cell [{:keys [player-item-on-cursor/item-put-sound]} eid cell c]
  (let [entity @eid
        inventory (:entity/inventory entity)
        item-in-cell (get-in inventory cell)
        item-on-cursor (:entity/item-on-cursor entity)]
    (cond
     ; PUT ITEM IN EMPTY CELL
     (and (not item-in-cell)
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (set-item c eid cell item-on-cursor)
      (fsm/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (stack-item c eid cell item-on-cursor)
      (fsm/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (sound/play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (remove-item c eid cell)
      (set-item c eid cell item-on-cursor)
      (fsm/event c eid :dropped-item)
      (fsm/event c eid :pickup-item item-in-cell)))))

(defmethod clicked-inventory-cell :player-item-on-cursor
  [[_ {:keys [eid] :as data}] cell c]
  (clicked-cell data eid cell c))

(defmethod clicked-inventory-cell :player-idle
  [[_ {:keys [eid player-idle/pickup-item-sound]}] cell c]
  ; TODO no else case
  (when-let [item (get-in (:entity/inventory @eid) cell)]
    (sound/play pickup-item-sound)
    (fsm/event c eid :pickup-item item)
    (remove-item c eid cell)))
