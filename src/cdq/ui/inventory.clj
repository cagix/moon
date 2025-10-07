(ns cdq.ui.inventory
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Image)
           (com.badlogic.gdx.scenes.scene2d.utils TextureRegionDrawable)))

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
        drawable (doto (TextureRegionDrawable. texture-region)
                   (.setMinSize (float cell-size) (float cell-size)))]
    (Image/.setDrawable image-widget drawable)
    (tooltip/add! cell-widget tooltip-text)))

(defn remove-item! [inventory-window cell]
  (let [cell-widget (window->cell inventory-window cell)
        image-widget (group/find-actor cell-widget "image-widget")]
    (Image/.setDrawable image-widget (:background-drawable (actor/user-object image-widget)))
    (tooltip/remove! cell-widget)))
