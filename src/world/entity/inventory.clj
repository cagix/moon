(ns world.entity.inventory
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.ui :as ui]
            [clojure.gdx.ui.actor :as a]
            [clojure.gdx.ui.stage-screen :refer [stage-get]]
            [core.component :refer [defsystem defc do! effect!] :as component]
            [core.property :as property]
            [data.grid2d :as g2d]
            [utils.core :refer [find-first]]
            [world.entity :as entity]
            [world.entity.modifiers :refer [mod-info-text]]
            [world.entity.state :as entity-state]
            [world.player :refer [world-player]]
            [world.widgets :refer [world-widgets]]))

(def ^:private empty-inventory
  (->> #:inventory.slot{:bag      [6 4]
                        :weapon   [1 1]
                        :shield   [1 1]
                        :helm     [1 1]
                        :chest    [1 1]
                        :leg      [1 1]
                        :glove    [1 1]
                        :boot     [1 1]
                        :cloak    [1 1]
                        :necklace [1 1]
                        :rings    [2 1]}
       (map (fn [[slot [width height]]]
              [slot (g2d/create-grid width height (constantly nil))]))
       (into {})))

(defc :item/slot
  {:data (apply vector :enum (keys empty-inventory))})

(defc :item/modifiers
  {:data [:components-ns :modifier]
   :let modifiers}
  (component/info [_]
    (when (seq modifiers)
      (mod-info-text modifiers))))

(property/def :properties/items
  {:schema [:property/pretty-name
            :entity/image
            :item/slot
            [:item/modifiers {:optional true}]]
   :overview {:title "Items"
              :columns 20
              :image/scale 1.1
              :sort-by-fn #(vector (if-let [slot (:item/slot %)]
                                     (name slot)
                                     "")
                             (name (:property/id %)))}})

(def ^:private body-props
  {:width 0.75
   :height 0.75
   :z-order :z-order/on-ground})

(defc :tx/item
  (do! [[_ position item]]
    [[:e/create position body-props {:entity/image (:entity/image item)
                                     :entity/item item
                                     :entity/clickable {:type :clickable/item
                                                        :text (:property/pretty-name item)}}]]))

(defn- cells-and-items [inventory slot]
  (for [[position item] (slot inventory)]
    [[slot position] item]))

(defn valid-slot? [[slot _] item]
  (or (= :inventory.slot/bag slot)
      (= (:item/slot item) slot)))

(defn- applies-modifiers? [[slot _]]
  (not= :inventory.slot/bag slot))

(defn stackable? [item-a item-b]
  (and (:count item-a)
       (:count item-b) ; this is not required but can be asserted, all of one name should have count if others have count
       (= (:property/id item-a) (:property/id item-b))))

(defn- set-item [{:keys [entity/id] :as entity*} cell item]
  (let [inventory (:entity/inventory entity*)]
    (assert (and (nil? (get-in inventory cell))
                 (valid-slot? cell item))))
  [[:e/assoc-in id (cons :entity/inventory cell) item]
   (when (applies-modifiers? cell)
     [:tx/apply-modifiers id (:item/modifiers item)])
   (when (:entity/player? entity*)
     [:tx/set-item-image-in-widget cell item])])

(defn- remove-item [{:keys [entity/id] :as entity*} cell]
  (let [item (get-in (:entity/inventory entity*) cell)]
    (assert item)
    [[:e/assoc-in id (cons :entity/inventory cell) nil]
     (when (applies-modifiers? cell)
       [:tx/reverse-modifiers id (:item/modifiers item)])
     (when (:entity/player? entity*)
       [:tx/remove-item-from-widget cell])]))

(defc :tx/set-item
  (do! [[_ entity cell item]]
    (set-item @entity cell item)))

(defc :tx/remove-item
  (do! [[_ entity cell]]
    (remove-item @entity cell)))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [entity cell]
  (let [item (get-in (:entity/inventory @entity) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (remove-item! entity cell)
       (set-item! entity cell (update item :count dec)))
      (remove-item! entity cell))))

; TODO no items which stack are available
(defn- stack-item [entity* cell item]
  (let [cell-item (get-in (:entity/inventory entity*) cell)]
    (assert (stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (remove-item entity* cell)
            (set-item entity* cell (update cell-item :count + (:count item))))))

(defc :tx/stack-item
  (do! [[_ entity cell item]]
    (stack-item @entity cell item)))

(defn- try-put-item-in [entity* slot item]
  (let [inventory (:entity/inventory entity*)
        cells-items (cells-and-items inventory slot)
        [cell _cell-item] (find-first (fn [[_cell cell-item]] (stackable? item cell-item))
                                      cells-items)]
    (if cell
      (stack-item entity* cell item)
      (when-let [[empty-cell] (find-first (fn [[_cell item]] (nil? item))
                                          cells-items)]
        (set-item entity* empty-cell item)))))

(defn- pickup-item [entity* item]
  (or
   (try-put-item-in entity* (:item/slot item)   item)
   (try-put-item-in entity* :inventory.slot/bag item)))

(defc :tx/pickup-item
  (do! [[_ entity item]]
    (pickup-item @entity item)))

(defn can-pickup-item? [entity* item]
  (boolean (pickup-item entity* item)))

(defc :entity/inventory
  {:data [:one-to-many :properties/items]}
  (entity/create [[_ items] eid]
    (cons [:e/assoc eid :entity/inventory empty-inventory]
          (for [item items]
            [:tx/pickup-item eid item]))))

; Items are also smaller than 48x48 all of them
; so wasting space ...
; can maybe make a smaller textureatlas or something...

(def ^:private cell-size 48)
(def ^:private droppable-color    [0   0.6 0 0.8])
(def ^:private not-allowed-color  [0.6 0   0 0.8])

(defn- draw-cell-rect [player-entity* x y mouseover? cell]
  (g/draw-rectangle x y cell-size cell-size :gray)
  (when (and mouseover?
             (= :player-item-on-cursor (entity-state/state-k player-entity*)))
    (let [item (:entity/item-on-cursor player-entity*)
          color (if (valid-slot? cell item)
                  droppable-color
                  not-allowed-color)]
      (g/draw-filled-rectangle (inc x) (inc y) (- cell-size 2) (- cell-size 2) color))))

; TODO why do I need to call getX ?
; is not layouted automatically to cell , use 0/0 ??
; (maybe (.setTransform stack true) ? , but docs say it should work anyway
(defn- draw-rect-actor []
  (ui/widget
   (fn [this]
     (draw-cell-rect @world-player
                     (a/x this)
                     (a/y this)
                     (a/mouseover? this (g/gui-mouse-position))
                     (a/id (a/parent this))))))

(defsystem clicked-inventory-cell [_ cell])
(defmethod clicked-inventory-cell :default [_ cell])

(defn- player-clicked-inventory [cell]
  (clicked-inventory-cell (entity-state/state-obj @world-player) cell))

(defn- ->cell [slot->background slot & {:keys [position]}]
  (let [cell [slot (or position [0 0])]
        image-widget (ui/image-widget (slot->background slot) {:id :image})
        stack (ui/stack [(draw-rect-actor) image-widget])]
    (a/set-name! stack "inventory-cell")
    (a/set-id! stack cell)
    (a/add-listener! stack (proxy [com.badlogic.gdx.scenes.scene2d.utils.ClickListener] []
                             (clicked [event x y]
                               (effect! (player-clicked-inventory cell)))))
    stack))

(defn- slot->background []
  (let [sheet (g/sprite-sheet "images/items.png" 48 48)]
    (->> #:inventory.slot {:weapon   0
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
         (map (fn [[slot y]]
                (let [drawable (ui/texture-region-drawable (:texture-region (g/sprite sheet [21 (+ y 2)])))]
                  (ui/set-min-size! drawable cell-size)
                  [slot
                   (ui/tinted-drawable drawable (g/->color 1 1 1 0.4))])))
         (into {}))))

(import 'com.badlogic.gdx.scenes.scene2d.ui.Table)

; TODO move together with empty-inventory definition ?
(defn- redo-table! [^Table table slot->background]
  ; cannot do add-rows, need bag :position idx
  (let [cell (fn [& args] (apply ->cell slot->background args))]
    (.clear table) ; no need as we create new table ... TODO
    (doto table .add .add
      (.add (cell :inventory.slot/helm))
      (.add (cell :inventory.slot/necklace)) .row)
    (doto table .add
      (.add (cell :inventory.slot/weapon))
      (.add (cell :inventory.slot/chest))
      (.add (cell :inventory.slot/cloak))
      (.add (cell :inventory.slot/shield)) .row)
    (doto table .add .add
      (.add (cell :inventory.slot/leg)) .row)
    (doto table .add
      (.add (cell :inventory.slot/glove))
      (.add (cell :inventory.slot/rings :position [0 0]))
      (.add (cell :inventory.slot/rings :position [1 0]))
      (.add (cell :inventory.slot/boot)) .row)
    ; TODO add separator
    (doseq [y (range (g2d/height (:inventory.slot/bag empty-inventory)))]
      (doseq [x (range (g2d/width (:inventory.slot/bag empty-inventory)))]
        (.add table (cell :inventory.slot/bag :position [x y])))
      (.row table))))

(defn ->inventory-window [{:keys [slot->background]}]
  (let [table (ui/table {:id ::table})]
    (redo-table! table slot->background)
    (ui/window {:title "Inventory"
                :id :inventory-window
                :visible? false
                :pack? true
                :position [(g/gui-viewport-width)
                           (g/gui-viewport-height)]
                :rows [[{:actor table :pad 4}]]})))

(defn ->inventory-window-data [] (slot->background))

(defn- get-inventory []
  {:table (::table (get (:windows (stage-get)) :inventory-window))
   :slot->background (:slot->background world-widgets)})

(defc :tx/set-item-image-in-widget
  (do! [[_ cell item]]
    (let [{:keys [table]} (get-inventory)
          cell-widget (get table cell)
          image-widget (get cell-widget :image)
          drawable (ui/texture-region-drawable (:texture-region (:entity/image item)))]
      (ui/set-min-size! drawable cell-size)
      (ui/set-drawable! image-widget drawable)
      (ui/add-tooltip! cell-widget #(component/info-text item))
      nil)))

(defc :tx/remove-item-from-widget
  (do! [[_ cell]]
    (let [{:keys [table slot->background]} (get-inventory)
          cell-widget (get table cell)
          image-widget (get cell-widget :image)]
      (ui/set-drawable! image-widget (slot->background (cell 0)))
      (ui/remove-tooltip! cell-widget)
      nil)))

(defn inventory-window []
  (get (:windows (stage-get)) :inventory-window))