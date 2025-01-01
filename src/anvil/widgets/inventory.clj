(ns anvil.widgets.inventory
  (:require [anvil.entity :as entity]
            [cdq.inventory :refer [empty-inventory] :as inventory]
            [clojure.gdx :as gdx]
            [clojure.gdx.scene2d.actor :refer [user-object] :as actor]
            [clojure.utils :refer [defn-impl]]
            [data.grid2d :as g2d]
            [gdl.context :as c]
            [gdl.info :as info]
            [gdl.ui :refer [set-drawable!
                            ui-widget
                            texture-region-drawable
                            image-widget
                            ui-stack
                            add-tooltip!
                            remove-tooltip!]
             :as ui]
            [gdl.val-max :as val-max]
            [gdl.ui.utils :as scene2d.utils]))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [c player-entity x y mouseover? cell]
  (c/rectangle c x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity/state-k player-entity)))
    (let [item (:entity/item-on-cursor player-entity)
          color (if (inventory/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (c/filled-rectangle c (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui-widget
   (fn [this {:keys [cdq.context/player-eid] :as c}]
     (draw-cell-rect c
                     @player-eid
                     (actor/x this)
                     (actor/y this)
                     (actor/hit this (c/mouse-position c))
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
  (c/from-sprite-sheet c
                       (c/sprite-sheet c "images/items.png" 48 48)
                       (slot->sprite-idx slot)))

(defn- slot->background [c slot]
  (let [drawable (-> (slot->sprite c slot)
                     :texture-region
                     texture-region-drawable)]
    (scene2d.utils/set-min-size! drawable cell-size)
    (scene2d.utils/tint drawable (gdx/color 1 1 1 0.4))))

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

(defn create [{:keys [gdl.context/viewport] :as c}]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [(:width viewport) (:height viewport)]
              :rows [[{:actor (inventory-table c)
                       :pad 4}]]}))

(defn- cell-widget [c cell]
  (get (::table (get (:windows (c/stage c)) :inventory-window)) cell))

(defn- set-item-image-in-widget [c cell item]
  (let [cell-widget (cell-widget c cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (scene2d.utils/set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text % item))))

(defn- remove-item-from-widget [c cell]
  (let [cell-widget (cell-widget c cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background c (cell 0)))
    (remove-tooltip! cell-widget)))

(defn-impl entity/notify-controller-item-set [context entity cell item]
  (when (:entity/player? entity)
    (set-item-image-in-widget context cell item)))

(defn-impl entity/notify-controller-item-removed [context entity cell]
  (when (:entity/player? entity)
    (remove-item-from-widget context cell)))
