(ns cdq.ui.inventory
  (:require [gdl.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.ui.image :as image]
            [com.badlogic.gdx.scenes.scene2d.utils.drawable :as drawable]))

(defn- find-cell [group cell]
  (first (filter #(= (actor/user-object % ) cell)
                 (group/children group))))

(defn- window->cell [inventory-window cell]
  (-> inventory-window
      (group/find-actor "inventory-cell-table")
      (find-cell cell)))

(defn set-item! [inventory-window cell {:keys [texture-region tooltip-text]}]
  (let [cell-widget (window->cell inventory-window cell)
        image-widget (group/find-actor cell-widget "image-widget")
        cell-size (:cell-size (actor/user-object image-widget))
        drawable (drawable/create texture-region :width cell-size :height cell-size)]
    (image/set-drawable! image-widget drawable)
    (actor/add-tooltip! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (window->cell inventory-window cell)
        image-widget (group/find-actor cell-widget "image-widget")]
    (image/set-drawable! image-widget (:background-drawable (actor/user-object image-widget)))
    (actor/remove-tooltip! cell-widget)))

(defn cell-with-item? [actor]
  (and (actor/parent actor)
       (= "inventory-cell" (actor/get-name (actor/parent actor)))
       (actor/user-object (actor/parent actor))))
