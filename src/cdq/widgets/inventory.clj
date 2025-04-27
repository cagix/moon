(ns cdq.widgets.inventory
  (:require [cdq.entity :as entity]
            cdq.graphics
            [cdq.inventory :refer [empty-inventory] :as inventory]
            [cdq.info :as info]
            [cdq.graphics.shape-drawer :as sd]
            cdq.graphics.sprite
            [cdq.ui.actor :refer [user-object] :as actor]
            [cdq.data.grid2d :as g2d]
            [cdq.ui :refer [ui-widget
                            texture-region-drawable
                            image-widget
                            ui-stack
                            set-drawable!
                            add-tooltip!
                            remove-tooltip!]
             :as ui])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d.utils BaseDrawable TextureRegionDrawable)))

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
                     cdq.graphics/shape-drawer
                     cdq.graphics/ui-viewport]}]
     (draw-cell-rect shape-drawer
                     @player-eid
                     (actor/x this)
                     (actor/y this)
                     (actor/hit this (cdq.graphics/mouse-position ui-viewport))
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
  (cdq.graphics.sprite/from-sheet (cdq.graphics.sprite/sheet c "images/items.png" 48 48)
                                  (slot->sprite-idx slot)
                                  c))

(defn- slot->background [c slot]
  (let [drawable (-> (slot->sprite c slot)
                     :texture-region
                     texture-region-drawable)]
    (BaseDrawable/.setMinSize drawable
                              (float cell-size)
                              (float cell-size))
    (TextureRegionDrawable/.tint drawable
                                 (Color. (float 1) (float 1) (float 1) (float 0.4)))))

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
                             (entity/clicked-inventory-cell (entity/state-obj @player-eid)
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

(defn create [context position]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position position
              :rows [[{:actor (inventory-table context)
                       :pad 4}]]}))

(defn- inventory-cell-widget [{:keys [cdq.context/stage]} cell]
  (get (::table (get (:windows stage) :inventory-window)) cell))

(defn- set-item-image-in-widget [c cell item]
  (let [cell-widget (inventory-cell-widget c cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (BaseDrawable/.setMinSize drawable
                              (float cell-size)
                              (float cell-size))
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text % item))))

(defn- remove-item-from-widget [c cell]
  (let [cell-widget (inventory-cell-widget c cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background c (cell 0)))
    (remove-tooltip! cell-widget)))

(defn set-item [c eid cell item]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (when (:entity/player? entity)
      (set-item-image-in-widget c cell item))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))))

(defn remove-item [c eid cell]
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
(defn stack-item [c eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item c eid cell)
            (set-item c eid cell (update cell-item :count + (:count item))))))

(defn pickup-item [c eid item]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (stack-item c eid cell item)
      (set-item c eid cell item))))
