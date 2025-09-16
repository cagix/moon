(ns cdq.ui.dev-menu
  (:require [cdq.ui.menu :as menu]))

(def config
  '[[cdq.ui.dev-menu.update-labels.mouseover-entity-id/create "images/mouseover.png"]
    [cdq.ui.dev-menu.update-labels.elapsed-time/create "images/clock.png"]
    [cdq.ui.dev-menu.update-labels.paused/item]
    [cdq.ui.dev-menu.update-labels.ui-mouse-position/item]
    [cdq.ui.dev-menu.update-labels.world-mouse-position/item]
    [cdq.ui.dev-menu.update-labels.zoom/create "images/zoom.png"]
    [cdq.ui.dev-menu.update-labels.fps/create "images/fps.png"]])

(defn create
  [{:keys [ctx/graphics]
    :as ctx}
   {:keys [menus]}]
  (menu/create
   {:menus (for [menu menus]
             (update menu :items (fn [[f params]]
                                   (f ctx params))))
    :update-labels (let [textures (:ctx/textures graphics)
                         ->texture (fn [path]
                                     (assert (contains? textures path))
                                     (get textures path))]
                     (for [[sym icon] config
                           :let [avar (requiring-resolve sym)]]
                       (if icon
                         (avar (->texture icon))
                         @avar)))}))
