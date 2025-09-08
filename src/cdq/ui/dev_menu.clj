(ns cdq.ui.dev-menu
  (:require [cdq.ui.menu :as menu]))

(defn help-items [_ctx _params]
  [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}])

(defn reset-game-fn
  [{:keys [ctx/application-state
           ctx/config]}
   world-fn]
  (swap! application-state (requiring-resolve 'cdq.application.reset-game-state/reset-game-state!) world-fn))

(defn create
  [{:keys [ctx/textures] :as ctx} {:keys [menus]}]
  (menu/create
   {:menus (for [menu menus]
             (update menu :items (fn [[f params]]
                                   ((requiring-resolve f) ctx params))))
    :update-labels (let [->texture (fn [path]
                                     (assert (contains? textures path))
                                     (get textures path))]
                     [((requiring-resolve 'cdq.ui.dev-menu.update-labels.mouseover-entity-id/create) (->texture "images/mouseover.png"))
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.elapsed-time/create)        (->texture "images/clock.png"))
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.paused/item)
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.ui-mouse-position/item)
                      @(requiring-resolve 'cdq.ui.dev-menu.update-labels.world-mouse-position/item)
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.zoom/create) (->texture "images/zoom.png"))
                      ((requiring-resolve 'cdq.ui.dev-menu.update-labels.fps/create) (->texture "images/fps.png"))])}))
