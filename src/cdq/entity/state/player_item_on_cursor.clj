(ns cdq.entity.state.player-item-on-cursor
  (:require [anvil.controls :as controls]
            [anvil.entity :as entity]
            [anvil.skill :as skill]
            [cdq.context :as world]
            [cdq.inventory :as inventory]
            [clojure.component :as component]
            [clojure.gdx :refer [play button-just-pressed?]]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.utils :refer [safe-merge]]
            [gdl.context :as c :refer [play-sound]]
            [gdl.ui :refer [window-title-bar? button?]]
            [gdl.math.vector :as v]))

(defn create [[_ eid item] c]
  (safe-merge (c/build c :player-item-on-cursor/component)
              {:eid eid
               :item item}))

(defn cursor [_]
  :cursors/hand-grab)

(defn pause-game? [_]
  true)

(defn enter [[_ {:keys [eid item]}] c]
  (swap! eid assoc :entity/item-on-cursor item))

(defn exit [[_ {:keys [eid player-item-on-cursor/place-world-item-sound]}] c]
  ; at clicked-cell when we put it into a inventory-cell
  ; we do not want to drop it on the ground too additonally,
  ; so we dissoc it there manually. Otherwise it creates another item
  ; on the ground
  (let [entity @eid]
    (when (:entity/item-on-cursor entity)
      (play place-world-item-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (world/item c
                  (world/item-place-position c entity)
                  (:entity/item-on-cursor entity)))))

(defn manual-tick [[_ {:keys [eid]}] c]
  (when (and (button-just-pressed? c :left)
             (world/world-item? c))
    (entity/event c eid :drop-item)))

(defn render-below [[_ {:keys [item]}] entity c]
  (when (world/world-item? c)
    (c/draw-centered c
                     (:entity/image item)
                     (world/item-place-position c entity))))

(defn draw-gui-view [[_ {:keys [eid]}] c]
  (when (not (world/world-item? c))
    (c/draw-centered c
                     (:entity/image (:entity/item-on-cursor @eid))
                     (c/mouse-position c))))

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
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; STACK ITEMS
     (and item-in-cell
          (inventory/stackable? item-in-cell item-on-cursor))
     (do
      (play item-put-sound)
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/stack-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item))

     ; SWAP ITEMS
     (and item-in-cell
          (inventory/valid-slot? cell item-on-cursor))
     (do
      (play item-put-sound)
      ; need to dissoc and drop otherwise state enter does not trigger picking it up again
      ; TODO? coud handle pickup-item from item-on-cursor state also
      (swap! eid dissoc :entity/item-on-cursor)
      (entity/remove-item c eid cell)
      (entity/set-item c eid cell item-on-cursor)
      (entity/event c eid :dropped-item)
      (entity/event c eid :pickup-item item-in-cell)))))

(defn clicked-inventory-cell [[_ {:keys [eid] :as data}] cell c]
  (clicked-cell data eid cell c))
