(ns anvil.widgets.inventory
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.info :as info]
            [anvil.widgets :as widgets]
            [clojure.gdx.graphics.color :as color]
            [data.grid2d :as g2d]
            [gdl.app :as app]
            [gdl.context :as c]
            [gdl.stage :as stage]
            [gdl.ui :refer [set-drawable!
                            ui-widget
                            texture-region-drawable
                            image-widget
                            ui-stack
                            add-tooltip!
                            remove-tooltip!]
             :as ui]
            [gdl.val-max :as val-max]
            [gdl.ui.actor :refer [user-object] :as actor]
            [gdl.ui.utils :as scene2d.utils])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.utils ClickListener)))

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
          color (if (entity/valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (c/filled-rectangle c (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui-widget
   (fn [^Actor this]
     (let [{:keys [cdq.context/player-eid] :as c} @app/state]
       (draw-cell-rect c
                       @player-eid
                       (.getX this)
                       (.getY this)
                       (actor/hit this (c/mouse-position c))
                       (user-object (.getParent this)))))))

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
    (scene2d.utils/tint drawable (color/create 1 1 1 0.4))))

(defn- ->cell ^Actor [{:keys [cdq.context/player-eid] :as c} slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (image-widget (slot->background c slot)
                                   {:id :image})
        stack (ui-stack [(draw-rect-actor)
                         image-widget])]
    (.setName stack "inventory-cell")
    (.setUserObject stack cell)
    (.addListener stack (proxy [ClickListener] []
                          (clicked [event x y]
                            (component/clicked-inventory-cell (entity/state-obj @player-eid)
                                                              cell
                                                              c))))
    stack))

(defn- inventory-table [c]
  (let [table (ui/table {:id ::table})]
    (.clear table) ; no need as we create new table ... TODO
    (doto table .add .add
      (.add (->cell c :inventory.slot/helm))
      (.add (->cell c :inventory.slot/necklace)) .row)
    (doto table .add
      (.add (->cell c :inventory.slot/weapon))
      (.add (->cell c :inventory.slot/chest))
      (.add (->cell c :inventory.slot/cloak))
      (.add (->cell c :inventory.slot/shield)) .row)
    (doto table .add .add
      (.add (->cell c :inventory.slot/leg)) .row)
    (doto table .add
      (.add (->cell c :inventory.slot/glove))
      (.add (->cell c :inventory.slot/rings :position [0 0]))
      (.add (->cell c :inventory.slot/rings :position [1 0]))
      (.add (->cell c :inventory.slot/boot)) .row)
    (doseq [y (range (g2d/height (:inventory.slot/bag entity/empty-inventory)))]
      (doseq [x (range (g2d/width (:inventory.slot/bag entity/empty-inventory)))]
        (.add table (->cell c :inventory.slot/bag :position [x y])))
      (.row table))
    table))

(defn-impl widgets/inventory [{:keys [gdl.context/viewport] :as c}]
  (ui/window {:title "Inventory"
              :id :inventory-window
              :visible? false
              :pack? true
              :position [(:width viewport) (:height viewport)]
              :rows [[{:actor (inventory-table c)
                       :pad 4}]]}))

(defn- cell-widget [cell]
  (get (::table (stage/get-inventory)) cell))

(defn-impl widgets/set-item-image-in-widget [cell item]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)
        drawable (texture-region-drawable (:texture-region (:entity/image item)))]
    (scene2d.utils/set-min-size! drawable cell-size)
    (set-drawable! image-widget drawable)
    (add-tooltip! cell-widget #(info/text item))))

(defn-impl widgets/remove-item-from-widget [c cell]
  (let [cell-widget (cell-widget cell)
        image-widget (get cell-widget :image)]
    (set-drawable! image-widget (slot->background c (cell 0)))
    (remove-tooltip! cell-widget)))
